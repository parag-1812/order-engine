import { Link, Route, Routes, useLocation } from "react-router-dom";
import CustomerPage from "./pages/CustomerPage";
import KitchenPage from "./pages/KitchenPage";
import "./App.css";

function NavLink({ to, children }) {
  const location = useLocation();
  const isActive =
    to === "/" ? location.pathname === "/" : location.pathname.startsWith(to);

  return (
    <Link to={to} className={`nav-link ${isActive ? "active" : ""}`}>
      {children}
    </Link>
  );
}

function App() {
  return (
    <div className="app-layout">
      <header className="topbar">
        <div>
          <p className="kicker">Order Engine</p>
          <h1>Cloud Kitchen Control Center</h1>
        </div>
        <nav className="nav-tabs">
          <NavLink to="/">Customer</NavLink>
          <NavLink to="/kitchen">Kitchen</NavLink>
        </nav>
      </header>

      <main className="main-content">
        <p className="intro">
          Create, track, and process orders using your Spring backend APIs.
        </p>
        <Routes>
          <Route path="/" element={<CustomerPage />} />
          <Route path="/kitchen" element={<KitchenPage />} />
        </Routes>
      </main>
      <footer className="footer-note">
        Frontend connected to <code>/orders</code> endpoints.
      </footer>
    </div>
  );
}

export default App;
