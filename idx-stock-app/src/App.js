import * as React from 'react';
import logo from './logo.svg';
import './App.css';
import StockLastUpdate from './StockLastUpdate';
import TestEventSource from './TestEventSource';
import Appbar from './Appbar';

function App() {
  return (
    <div className="App">
      <Appbar />
      <StockLastUpdate />
    </div>
  );
}

export default App;
