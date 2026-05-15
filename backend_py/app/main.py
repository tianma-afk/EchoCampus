from fastapi import FastAPI
import uvicorn

app = FastAPI()

@app.get("/")
def root():
    return {"message": "Hello EchoCampus"}

# http://127.0.0.1:8000/docs
