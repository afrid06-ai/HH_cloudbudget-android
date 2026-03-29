import logging
import os
from contextlib import asynccontextmanager

from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

load_dotenv()

from app.routers import budget, credentials, dashboard, seed, sync, trends, waste

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

_scheduler = None


def _scheduled_firestore_sync() -> None:
    try:
        from app.services.firestore_sync import sync_to_firestore

        sync_to_firestore("all")
        logger.info("Scheduled Firestore sync completed")
    except Exception:
        logger.exception("Scheduled Firestore sync failed")


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _scheduler
    if os.getenv("ENABLE_SCHEDULER", "").lower() == "true":
        from apscheduler.schedulers.background import BackgroundScheduler

        hours = int(os.getenv("SYNC_INTERVAL_HOURS", "1"))
        _scheduler = BackgroundScheduler()
        _scheduler.add_job(
            _scheduled_firestore_sync,
            "interval",
            hours=max(1, hours),
            id="firestore_sync_all",
            replace_existing=True,
        )
        _scheduler.start()
        logger.info("APScheduler: Firestore sync every %s h (ENABLE_SCHEDULER=true)", hours)
    yield
    if _scheduler is not None:
        _scheduler.shutdown(wait=False)
        logger.info("APScheduler stopped")


app = FastAPI(
    title="CloudBudget API",
    description="Multi-Cloud Billing Tracker Backend — REST + Firestore sync for Android",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(dashboard.router, prefix="/api", tags=["Dashboard"])
app.include_router(budget.router, prefix="/api", tags=["Budget"])
app.include_router(waste.router, prefix="/api", tags=["Waste"])
app.include_router(trends.router, prefix="/api", tags=["Trends"])
app.include_router(sync.router, prefix="/api", tags=["Sync"])
app.include_router(seed.router, prefix="/api", tags=["Seed"])
app.include_router(credentials.router, prefix="/api", tags=["Credentials & Per-User Sync"])


@app.get("/")
def root():
    return {"message": "CloudBudget API is running", "version": "1.0.0"}


@app.get("/health")
def health_check():
    return {"status": "healthy"}
