# Regretn't Backend

Node.js + TypeScript + Express + MongoDB backend for Regretn't app.

## Features
- JWT-based authentication (register/login with PIN).
- CRUD for Goals with baseline, evidence, completion tracking.
- Blocked apps list per user.
- Rate limiting for auth & goal completion endpoints.
- MongoDB connection (local, Atlas).
- Configurable CORS (allowed origins via env).
- Winston logging + morgan HTTP logs.
- Graceful shutdown.

## Requirements
- Node.js >= 18
- npm or yarn
- MongoDB (local or Atlas)

## Setup

1. Clone repository and install dependencies:

   ```bash
   cd backend
   npm install
