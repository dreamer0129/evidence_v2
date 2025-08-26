# GEMINI Project Analysis: Blockchain-Based Evidence Management System

## Project Overview

This project is a full-stack, blockchain-based evidence management system. It provides a secure and transparent platform for managing digital evidence, leveraging the immutability of blockchain to ensure data integrity.

The architecture consists of three main components:

*   **Backend:** A Java-based backend built with the Spring Boot framework. It handles business logic, user authentication, and interaction with the blockchain.
*   **Frontend:** A modern, responsive web application built with Next.js and React. It provides the user interface for interacting with the system.
*   **Smart Contracts:** Solidity-based smart contracts deployed on an Ethereum-compatible blockchain. These contracts manage the evidence records and ensure their integrity.

## Building and Running

### Prerequisites

*   Node.js (>= v20.18.3)
*   Yarn (v1 or v2+)
*   Java (>= 21)
*   Spring Boot (>= 3.4.9)

### Installation

```bash
yarn install
```

### Development Servers

**1. Start the local blockchain:**

```bash
yarn chain
```

**2. Deploy the smart contracts:**

```bash
yarn deploy
```

**3. Start the Next.js frontend:**

```bash
yarn start
```

The frontend will be available at `http://localhost:3000`.

**4. Start the Spring Boot backend:**

```bash
mvn spring-boot:run
```

The backend API will be available at `http://localhost:8080`.

### Testing

**Run smart contract tests:**

```bash
yarn hardhat:test
```

## Development Conventions

*   **Monorepo:** The project uses Yarn workspaces to manage the `hardhat` and `nextjs` packages as a monorepo.
*   **Smart Contracts:** Solidity contracts are located in the `hardhat/contracts` directory. Deployment scripts are in `hardhat/deploy`.
*   **Frontend:** The Next.js application is in the `nextjs` directory. It uses `scaffold-eth-2` for the initial setup and includes custom hooks and components for interacting with the smart contracts.
*   **Backend:** The Spring Boot application is in the `src/main/java` directory. It uses Web3j to interact with the smart contracts and automatically generates Java classes from the contract ABIs.
*   **Code Style:** The project uses Prettier and ESLint for code formatting and linting. Run `yarn format` and `yarn lint` to check and fix code style.
