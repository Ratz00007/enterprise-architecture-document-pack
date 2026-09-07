"""OpenAI-compatible sandbox provider for the Acme GenAI gateway (dev only).

LiteLLM (ADR-011) routes to this service in dev so the whole stack runs with
no external vendor and no data leaving the machine. It enforces the
data-classification policy before anything reaches a model: direct
identifiers are rejected outright, and every response is advisory only.
"""

import json
import os
import re
import time
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

POLICY_PATH = Path(os.environ.get("POLICY_PATH", "policy.json"))
POLICY = json.loads(POLICY_PATH.read_text())
PII_RULES = json.loads((Path(__file__).parent / "pii-rules.json").read_text())
DENY_PATTERNS = [re.compile(rule["pattern"]) for rule in PII_RULES["deny_patterns"]]

app = FastAPI(title="Acme GenAI sandbox provider", version="1.0.0")


class Message(BaseModel):
    role: str
    content: str


class ChatCompletionRequest(BaseModel):
    model: str
    messages: list[Message]
    temperature: float | None = None
    max_tokens: int | None = None


def contains_denied_data(text: str) -> bool:
    return any(pattern.search(text) for pattern in DENY_PATTERNS)


def advisory_answer(prompt: str) -> str:
    return (
        "SANDBOX ADVISORY (dev gateway, no real model): "
        f"request of {len(prompt)} characters passed the data-classification "
        "allow-list. All gateway output is advisory; a human decides (ADR-006)."
    )


@app.get("/health")
def health() -> dict:
    return {"status": "UP", "policy_version": POLICY["version"]}


@app.post("/v1/chat/completions")
def chat_completions(request: ChatCompletionRequest) -> dict:
    prompt = "\n".join(message.content for message in request.messages)
    if contains_denied_data(prompt):
        raise HTTPException(
            status_code=422,
            detail="Blocked by data-classification policy: direct identifiers are "
            "never sent to a model (ADR-011, ADR-017).",
        )
    return {
        "id": f"sandbox-{int(time.time())}",
        "object": "chat.completion",
        "model": request.model,
        "choices": [
            {
                "index": 0,
                "message": {"role": "assistant", "content": advisory_answer(prompt)},
                "finish_reason": "stop",
            }
        ],
        "usage": {
            "prompt_tokens": len(prompt.split()),
            "completion_tokens": 0,
            "total_tokens": len(prompt.split()),
        },
    }
