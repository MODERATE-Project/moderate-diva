from fastapi import FastAPI
import uvicorn

from api import auth, stats
from core.stats import compute_stats
from core.settings import settings

import threading

app = FastAPI(
    title=settings.app_name,
    description=settings.app_description,
    version=settings.version
)

# The requests that are used to authenticate the users
app.include_router(auth.router,
                   tags=["Auth"],
                   responses={404: settings.response_404}
)

# The requests that are used to retrieve the messages statistics are handled by the stats router.
app.include_router(stats.router,
                   tags=["Get Report"],
                   responses={404: settings.response_404}
)

app.mount("/api/", app)

if __name__ == "__main__":
    """Main thread of the backend that has to guarantee the execution
    of the REST requests sent by the client applications.
    Meanwhile, it has to compute the statistics of the messages that comes from Kafka.
    These updates are in charge of a parallel daemon thread.
    """

    th = threading.Thread(target=compute_stats, args=(), daemon=True)
    th.start()

    uvicorn.run(app=app, host="0.0.0.0", port=settings.port)