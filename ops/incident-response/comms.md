# Incident Comms Templates

## Initial notification (Sev-1)

```
:rotating_light: SEV-1 — <one-line summary>

What we know: <2-3 sentences, facts only>
Customer impact: <who, how many, what can't they do>
Current action: <what the team is doing right now>
Next update: <time, e.g. "in 15 minutes">
Incident commander: <name>
```

## Status update

```
:arrows_counterclockwise: SEV-1 UPDATE — <time>

What changed since last update: <bullets>
Current state: <still degraded / partial recovery / monitoring>
Next update: <time>
```

## Resolution

```
:white_check_mark: SEV-1 RESOLVED — <time>

What happened: <2-3 sentence summary>
How we fixed it: <bullet list>
Customer impact: <duration + scope>
Follow-up: <link to postmortem doc, ETA>
```

## Postmortem template

```
# <incident title>

## Summary
<1 paragraph>

## Timeline (UTC)
- HH:MM — <event>
- HH:MM — <event>

## Root cause
<2-3 paragraphs>

## Contributing factors
- <bullet>

## What went well
- <bullet>

## What went poorly
- <bullet>

## Action items
| # | Action | Owner | Due |
|---|--------|-------|-----|
| 1 | <action> | <name> | <date> |
