import { useState } from "react";
import axios from "axios";

function App() {
  const [items, setItems] = useState([{ ingredientId: "", quantity: "" }]);
  const [response, setResponse] = useState(null);

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
        "http://localhost:8080/orders?customerId=101",
        { items }
      );
      setResponse(res.data);
    } catch (error) {
      setResponse(error.response?.data || { message: "Error occurred" });
    }
  };

  return (
    <div style={{ padding: "40px", fontFamily: "Arial" }}>
      <h1>Cloud Kitchen - Create Order</h1>

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
    </div>
  );
}

export default App;
