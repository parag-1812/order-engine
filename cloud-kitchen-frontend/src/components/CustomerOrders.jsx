import { useState } from "react";
import axios from "axios";

function CustomerOrders() {
  const [orders, setOrders] = useState([]);
  const customerId = 101;

  const fetchOrders = async () => {
    try {
      const res = await axios.get(
        `http://localhost:8080/orders/customer/${customerId}`
      );
      setOrders(res.data);
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <div>
      <h3>My Orders</h3>
      <button onClick={fetchOrders}>Load My Orders</button>

      {orders.map((order) => (
        <div
          key={order.orderId}
          style={{
            border: "1px solid gray",
            padding: "10px",
            marginTop: "10px"
          }}
        >
          <p><b>Order ID:</b> {order.orderId}</p>
          <p><b>Status:</b> {order.status}</p>
          <p><b>Total Price:</b> ₹{order.totalPrice}</p>
          <p><b>Total Prep Time:</b> {order.totalPrepTime} mins</p>
        </div>
      ))}
    </div>
  );
}

export default CustomerOrders;
