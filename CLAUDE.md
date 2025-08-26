# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🏗️ Architecture Overview

This is a full-stack blockchain evidence management system with three main components:

1. **Hardhat** (`/hardhat/`) - Ethereum smart contract development
   - Solidity contracts in `contracts/`
   - TypeScript deployment scripts in `deploy/`
   - Hardhat tests in `test/`

2. **Next.js Frontend** (`/nextjs/`) - Web3 DApp interface
   - React components in `components/`
   - Custom Web3 hooks in `hooks/scaffold-eth/`
   - Scaffold-ETH 2 based architecture

3. **Spring Boot Backend** (`/src/`) - Java REST API
   - Main application: `EvidenceApplication.java`
   - Maven-based build with Java 24
   - Spring Security enabled

## 🚀 Development Commands

### Workspace Management

```bash
yarn install          # Install all dependencies (root + workspaces)
```

### Blockchain Development (Hardhat)

```bash
yarn chain           # Start local Ethereum network
```

### Smart Contract Operations

```bash
yarn compile         # Compile Solidity contracts
yarn deploy          # Deploy contracts to local network
yarn hardhat:test    # Run contract tests with gas reporting
yarn account         # List local accounts
yarn account:generate # Generate new test account
```

### Frontend Development (Next.js)

```bash
yarn start           # Start Next.js dev server (localhost:3000)
yarn next:build      # Build for production
yarn next:lint       # Run ESLint on frontend
yarn next:check-types # TypeScript type checking
```

### Backend Development (Spring Boot)

```bash
mvn spring-boot:run  # Start Spring Boot server (localhost:8080)
mvn test             # Run backend tests
mvn compile          # Compile Java code
mvn web3j:generate-sources  # Genreate Java class from solidity.  
```

### Code Quality

```bash
yarn format          # Format all code (Prettier)
yarn lint            # Lint all code (ESLint)
yarn hardhat:check-types # Type check Hardhat code
yarn next:check-types    # Type check Next.js code
```

## 🔧 Key Configuration Files

- `scaffold.config.ts` - Frontend blockchain configuration (networks, RPC URLs)
- `hardhat.config.ts` - Hardhat network and plugin configuration
- `pom.xml` - Maven build configuration for Spring Boot
- `application.properties` - Spring Boot application settings

## 🎯 Development Workflow

1. **Start local blockchain**: `yarn chain` (Terminal 1)
2. **Deploy contracts**: `yarn deploy` (Terminal 2)  
3. **Start frontend**: `yarn start` (Terminal 3)
4. **Start backend**: `mvn spring-boot:run` (Terminal 4)

## 📁 Key Directories

- `hardhat/contracts/` - Smart contract source files
- `hardhat/deploy/` - Contract deployment scripts
- `nextjs/app/` - Next.js page routes and components
- `nextjs/hooks/scaffold-eth/` - Custom Web3 React hooks
- `src/main/java/cn/edu/gfkd/evidence/` - Spring Boot application code

## 🔗 Network Configuration

The system targets `hardhat` network by default. Configuration is managed in:

- Frontend: `nextjs/scaffold.config.ts`
- Hardhat: `hardhat/hardhat.config.ts`

Contracts are automatically deployed to localhost:8545 and frontend hot-reloads with contract changes.
