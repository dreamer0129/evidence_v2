# 🧾 Blockchain-Based Evidence Management System

> A secure, transparent, and tamper-proof evidence management system leveraging blockchain technology.

## 📖 Table of Contents

- [🧾 Blockchain-Based Evidence Management System](#-blockchain-based-evidence-management-system)
  - [📖 Table of Contents](#-table-of-contents)
  - [🧩 Project Overview](#-project-overview)
  - [🛠️ Tech Stack](#️-tech-stack)
  - [📁 Project Structure](#-project-structure)
  - [⚙️ Prerequisites](#️-prerequisites)
  - [🚀 Quick Start](#-quick-start)
    - [1. Install Dependencies](#1-install-dependencies)
    - [2. Run a local network in the first terminal:](#2-run-a-local-network-in-the-first-terminal)
    - [3. On a second terminal, deploy the test contract in hardhat local network](#3-on-a-second-terminal-deploy-the-test-contract-in-hardhat-local-network)
    - [4. On a third terminal, start your NextJS app:](#4-on-a-third-terminal-start-your-nextjs-app)
    - [5. Start Backend Server](#5-start-backend-server)
  - [🧪 Testing](#-testing)
  - [🛠️ Development](#️-development)
    - [1. Smart Contract Development](#1-smart-contract-development)
    - [2. Frontend Development](#2-frontend-development)
    - [3. Backend Development](#3-backend-development)
  - [📋 Features](#-features)

## 🧩 Project Overview

This project is a full-stack implementation of a blockchain-based evidence management system. It utilizes **Spring Boot (Java 24)** for the backend, **Hardhat (Solidity)** for smart contracts, and **Next.js** for the frontend, creating a robust and decentralized solution for handling digital evidence.

The system ensures the integrity and authenticity of digital evidence by storing cryptographic proofs on the Ethereum blockchain. Key evidence metadata is anchored on-chain, while the actual files are stored securely off-chain.

## 🛠️ Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Blockchain** | Hardhat, Solidity, Ethereum |
| **Frontend** | Next.js, RainbowKit, Wagmi, Viem, TypeScript |
| **Backend** | Spring Boot 3.5.5, Java 24, Web3j |
| **Database** | SQLite |
| **File Storage** | Local Filesystem |

## 📁 Project Structure

├── hardhat/ # Smart Contract Project
│ ├── contracts/ # Solidity smart contracts
│ ├── deploy/ # Deployment scripts
│ └── test/ # Contract tests
├── nextjs/ # Next.js DApp
│ ├── hooks/ # Custom Wagmi hooks
│ └── components/ # Reusable Web3 components
└── src/ # Spring Boot Application

## ⚙️ Prerequisites

Before running this project, ensure you have the following installed:

- **Node.js** (>= v20.18.3)
- **Yarn** (v1 or v2+)
- **Git**
- **Java** (>= 21)
- **Spring Boot** (>= 3.4.9)


## 🚀 Quick Start

Follow these steps to get the project running locally:

### 1. Install Dependencies

```bash
yarn install
```

### 2. Run a local network in the first terminal:

```bash
yarn chain
```

This command starts a local Ethereum network using Hardhat. The network runs on your local machine and can be used for testing and development. You can customize the network configuration in `hardhat/hardhat.config.ts`.

### 3. On a second terminal, deploy the test contract in hardhat local network

```bash
yarn deploy
```

### 4. On a third terminal, start your NextJS app:

```bash
yarn start
```
Visit your app on: `http://localhost:3000`. 

### 5. Start Backend Server
```bash
mvn springboot:run
```
the backend API will be available at http://localhost:8080.

## 🧪 Testing
Run the smart contract tests to verify everything is working correctly:

```bash
yarn hardhat:test
```

## 🛠️ Development

### 1. Smart Contract Development

Edit your contracts in hardhat/contracts/

Add deployment scripts in hardhat/deploy/

The frontend automatically adapts to contract changes with hot reload

### 2. Frontend Development 

The frontend is built with scaffold-eth-2

Custom hooks are available in nextjs/hooks/

Common components are located in nextjs/components/

### 3. Backend Development

Main application class: EvidenceApplication.java

Web3j generates Java classes from smart contracts automatically

File storage configuration in FileStorage service

## 📋 Features
- ✅ Contract Hot Reload: Frontend auto-adapts to smart contract changes
- 🪝 Custom Hooks: React hooks wrapper around Wagmi for simplified contract interactions
- 🧱 Reusable Components: Common Web3 components for rapid development
- 🔥 Burner Wallet & Local Faucet: Quick testing with local ETH
- 🔐 Multi-Wallet Support: Connect with various Ethereum wallets
- 📁 Secure File Storage: Off-chain evidence storage with on-chain verification
- 🔗 Blockchain Verification: Cryptographic proof of evidence integrity