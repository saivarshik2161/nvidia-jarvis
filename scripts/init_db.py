#!/usr/bin/env python3
"""Database initialization script."""
import asyncio
import sys
import os

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'backend'))

from app.db.database import init_db


async def main():
    print("Initializing database...")
    await init_db()
    print("Database initialized successfully!")


if __name__ == "__main__":
    asyncio.run(main())