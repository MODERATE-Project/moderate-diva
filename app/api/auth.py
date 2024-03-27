from fastapi import APIRouter
from fastapi.responses import JSONResponse

from core.auth import check_token, get_user
router = APIRouter()

@router.get("/auth")
async def get_config(token: str):
    """This API checks if the user is authorized or not.

    Returns:
        JSONResponse: object containing the headers and the body of the request.
    """
    headers = {"Access-Control-Allow-Origin": "*"}

    content = {"authorized": check_token(token)}
    if content["authorized"]:
        content["user"] = get_user(token)
        
    return JSONResponse(content=content, headers=headers)