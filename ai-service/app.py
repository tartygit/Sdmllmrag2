from flask import Flask, request, jsonify, Response
from pipeline.rag_pipeline import LocalRAGPipeline
import json
import time

app = Flask(__name__)
rag = LocalRAGPipeline()

@app.route("/api/ai/analyze", methods=["POST"])
def analyze():
    data = request.json
    file_path = data.get("filePath")
    if not file_path:
        return jsonify({"error": "filePath is required"}), 400
    return jsonify(rag.extract_ai_recommendations(file_path))

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
