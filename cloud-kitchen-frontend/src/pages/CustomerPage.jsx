import CreateOrder from "../components/CreateOrder";
import CustomerOrders from "../components/CustomerOrders";

function CustomerPage() {
  return (
    <>
      <h2>Customer Dashboard</h2>
      <CreateOrder />
      <CustomerOrders />
    </>
  );
}

export default CustomerPage;
