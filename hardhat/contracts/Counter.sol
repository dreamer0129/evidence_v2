//SPDX-License-Identifier: MIT
pragma solidity >=0.8.0 <0.9.0;

contract Counter {
    uint public x;

    constructor(uint _x) {
        x = _x;
    }

    function increment() external {
        x++;
        emit XUpdate(x - 1, x);
    }

    function decrement() external {
        x--;
        emit XUpdate(x + 1, x);
    }

    event XUpdate(uint oldValue, uint newValue);
}
