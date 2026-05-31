from fastapi import APIRouter

from schemas.search import SearchParams

router = APIRouter(prefix="/search", tags=["search"])
@router.post("/")
async def search_receive(params: SearchParams):
    return None # TODO