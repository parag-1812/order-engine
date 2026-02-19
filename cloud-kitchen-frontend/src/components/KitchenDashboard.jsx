import { useState } from "react";
import axios from "axios";

function KitchenDashboard() {
  const [orders, setOrders] = useState([]);
  const kitchenId = 1;

  const fetchOrders = async () => {
    const res = await axios.get(
      `http://localhost:8080/orders/kitchen/${kitchenId}`
    );
    setOrders(res.data);
  };

  const updateStatus = async (orderId, status) => {
    await axios.patch(
      `http://localhost:8080/orders/${orderId}/status?status=${status}`
    );
    fetchOrders();
  };

  return (
    <div>
      <button onClick={fetchOrders}>Load Kitchen Orders</button>

      {orders.map((order) => (
        <div
          key={order.orderId}
          style={{
            border: "1px solid blue",
            padding: "10px",
            marginTop: "10px"
          }}
        >
          <p><b>Order ID:</b> {order.orderId}</p>
          <p><b>Status:</b> {order.status}</p>

          {order.status === "KITCHEN_ASSIGNED" && (
            <button onClick={() => updateStatus(order.orderId, "COOKING")}>
              Start Cooking
            </button>
          )}

          {order.status === "COOKING" && (
            <button onClick={() => updateStatus(order.orderId, "READY")}>
              Mark Ready
            </button>
          )}

          {order.status === "READY" && (
            <button onClick={() => updateStatus(order.orderId, "DELIVERED")}>
              Deliver
            </button>
          )}

          {order.status === "DELIVERED" && (
            <span style={{ color: "green", fontWeight: "bold" }}>
              Completed
            </span>
          )}
        </div>
      ))}
    </div>
  );
}

export default KitchenDashboard;
