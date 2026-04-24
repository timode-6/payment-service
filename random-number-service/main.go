package main

import (
	"encoding/json"
	"log"
	"math/rand"
	"net/http"
	"os"
	"time"
)

type RandomResponse struct {
	Number int  `json:"number"`
	IsEven bool `json:"isEven"`
}

type healthResponse struct {
	Status string `json:"status"`
}

func randomHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	src := rand.NewSource(time.Now().UnixNano())
	rng := rand.New(src)
	number := rng.Intn(10_000)

	resp := RandomResponse{
		Number: number,
		IsEven: number%2 == 0,
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(resp); err != nil {
		log.Printf("encode error: %v", err)
	}
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(healthResponse{Status: "UP"})
}

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8085"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/api/random", randomHandler)
	mux.HandleFunc("/health", healthHandler)

	addr := ":" + port
	log.Printf("random-number-service listening on %s", addr)
	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
