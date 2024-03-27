from fastapi import APIRouter, HTTPException
from fastapi.responses import JSONResponse

from core.settings import topic_stats
from core.auth import check_token

router = APIRouter()

@router.get("/report")
async def get_list(token: str):
    """This API returns the list of simulation/ai requests created by the users.
    In addition, for each request, it is also provided the current state of progress.

    Returns:
        JSONResponse: object containing the headers and the body of the response.
    """
    
    if not check_token(token):
        raise HTTPException(status_code=401, detail="Access denied")

    headers = {"Access-Control-Allow-Origin": "*"}
    return JSONResponse(content=topic_stats.get_report(), headers=headers)
