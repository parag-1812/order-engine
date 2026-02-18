import { useState } from "react";
import axios from "axios";

function App() {
  const [items, setItems] = useState([{ ingredientId: "", quantity: "" }]);
  const [response, setResponse] = useState(null);
  const [orders, setOrders] = useState([]);

  const customerId = 101;

  const handleChange = (index, field, value) => {
    const updatedItems = [...items];
    updatedItems[index][field] = value;
    setItems(updatedItems);
  };

  const addItem = () => {
    setItems([...items, { ingredientId: "", quantity: "" }]);
  };

  const submitOrder = async () => {
    try {
      const res = await axios.post(
        `http://localhost:8080/orders?customerId=${customerId}`,
        { items }
      );
      setResponse(res.data);
    } catch (error) {
      setResponse(error.response?.data || { message: "Error occurred" });
    }
  };

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
    <div style={{ padding: "40px", fontFamily: "Arial" }}>
      <h1>Cloud Kitchen</h1>

      <h2>Create Order</h2>

      {items.map((item, index) => (
        <div key={index} style={{ marginBottom: "10px" }}>
          <input
            type="number"
            placeholder="Ingredient ID"
            value={item.ingredientId}
            onChange={(e) =>
              handleChange(index, "ingredientId", Number(e.target.value))
            }
          />

          <input
            type="number"
            placeholder="Quantity"
            value={item.quantity}
            onChange={(e) =>
              handleChange(index, "quantity", Number(e.target.value))
            }
            style={{ marginLeft: "10px" }}
          />
        </div>
      ))}

      <button onClick={addItem}>Add Item</button>
      <button onClick={submitOrder} style={{ marginLeft: "10px" }}>
        Submit Order
      </button>

      {response && (
        <div style={{ marginTop: "20px" }}>
          <h3>Response:</h3>
          <pre>{JSON.stringify(response, null, 2)}</pre>
        </div>
      )}

      <hr />

      <h2>My Orders</h2>
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

export default App;
