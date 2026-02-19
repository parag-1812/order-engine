import { Routes, Route, Link } from "react-router-dom";
import CustomerPage from "./pages/CustomerPage";
import KitchenPage from "./pages/KitchenPage";

function App() {
  return (
    <div style={{ padding: "30px", fontFamily: "Arial" }}>
      <h1>Cloud Kitchen</h1>

      <nav style={{ marginBottom: "20px" }}>
        <Link to="/" style={{ marginRight: "15px" }}>
          Customer
        </Link>
        <Link to="/kitchen">
          Kitchen
        </Link>
      </nav>

      <Routes>
        <Route path="/" element={<CustomerPage />} />
        <Route path="/kitchen" element={<KitchenPage />} />
      </Routes>
    </div>
  );
}

export default App;
