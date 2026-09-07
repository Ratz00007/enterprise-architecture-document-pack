import { Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { FnolPage } from "./pages/FnolPage";
import { ClaimsListPage } from "./pages/ClaimsListPage";
import { ClaimDetailPage } from "./pages/ClaimDetailPage";

export function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<ClaimsListPage />} />
        <Route path="/claims/:id" element={<ClaimDetailPage />} />
        <Route path="/fnol" element={<FnolPage />} />
      </Routes>
    </Layout>
  );
}
