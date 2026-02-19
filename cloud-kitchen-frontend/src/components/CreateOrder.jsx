import { useState } from "react";
import axios from "axios";

function CreateOrder() {
  const [items, setItems] = useState([{ ingredientId: "", quantity: "" }]);
  const [response, setResponse] = useState(null);

  const customerId = 101;

  const handleChange = (index, field, value) => {
    const updated = [...items];
    updated[index][field] = value;
    setItems(updated);
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

  return (
    <div style={{ marginBottom: "30px" }}>
      <h3>Create Order</h3>

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
          <h4>Response:</h4>
          <pre>{JSON.stringify(response, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}

export default CreateOrder;
