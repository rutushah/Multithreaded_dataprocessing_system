package main

import (
	"bufio"
	"fmt"
	"log"
	"math/rand"
	"os"
	"sync"
	"time"
)

// =====================================================
// MODELS
// =====================================================

type Task struct {
	ID       int
	Data     string
	Priority int
}

type TaskResult struct {
	TaskID         int
	WorkerName     string
	Result         string
	ProcessingTime time.Duration
	CompletedAt    time.Time
}

func (r TaskResult) String() string {
	return fmt.Sprintf("[%s] Worker=%-10s | TaskID=%3d | Time=%4dms | Result=%s",
		r.CompletedAt.Format("15:04:05.000"),
		r.WorkerName,
		r.TaskID,
		r.ProcessingTime.Milliseconds(),
		r.Result,
	)
}

// =====================================================
// RESULT STORE
// =====================================================

type ResultStore struct {
	mu      sync.Mutex
	results []TaskResult
}

func NewResultStore() *ResultStore {
	return &ResultStore{}
}

func (rs *ResultStore) Add(result TaskResult) {
	rs.mu.Lock()
	defer rs.mu.Unlock()
	rs.results = append(rs.results, result)
}

func (rs *ResultStore) GetAll() []TaskResult {
	rs.mu.Lock()
	defer rs.mu.Unlock()
	snapshot := make([]TaskResult, len(rs.results))
	copy(snapshot, rs.results)
	return snapshot
}

func (rs *ResultStore) Count() int {
	rs.mu.Lock()
	defer rs.mu.Unlock()
	return len(rs.results)
}

func (rs *ResultStore) WriteToFile(filename string) error {
	rs.mu.Lock()
	snapshot := make([]TaskResult, len(rs.results))
	copy(snapshot, rs.results)
	rs.mu.Unlock()

	file, err := os.Create(filename)
	if err != nil {
		return fmt.Errorf("failed to create file %s: %w", filename, err)
	}
	defer file.Close()

	writer := bufio.NewWriter(file)

	_, err = fmt.Fprintf(writer, "=== Data Processing System Results ===\nTotal: %d\n\n", len(snapshot))
	if err != nil {
		return fmt.Errorf("write error: %w", err)
	}

	for _, r := range snapshot {
		_, err = fmt.Fprintln(writer, r.String())
		if err != nil {
			return fmt.Errorf("write error on result %d: %w", r.TaskID, err)
		}
	}

	if err = writer.Flush(); err != nil {
		return fmt.Errorf("flush error: %w", err)
	}

	log.Printf("Results written to %s\n", filename)
	return nil
}

// =====================================================
// WORKER
// =====================================================

func worker(name string, taskCh <-chan Task, store *ResultStore, wg *sync.WaitGroup) {
	defer wg.Done()

	log.Printf("[INFO]  %s started\n", name)

	completed := 0
	errors := 0

	for task := range taskCh {
		result, err := processTask(name, task)
		if err != nil {
			errors++
			log.Printf("[ERROR] %s failed on Task %d: %v\n", name, task.ID, err)
			continue
		}
		store.Add(result)
		completed++
		log.Printf("[INFO]  %s completed Task %d (%dms)\n",
			name, task.ID, result.ProcessingTime.Milliseconds())
	}

	log.Printf("[INFO]  %s finished. Completed=%d Errors=%d\n", name, completed, errors)
}

func processTask(workerName string, task Task) (TaskResult, error) {
	start := time.Now()

	// Simulate variable processing time based on priority
	delay := time.Duration(100+rand.Intn(400)+(task.Priority*20)) * time.Millisecond
	time.Sleep(delay)

	// Simulate occasional error (1 in 10 chance)
	if rand.Intn(10) == 0 {
		return TaskResult{}, fmt.Errorf("simulated failure for task %d", task.ID)
	}

	result := TaskResult{
		TaskID:         task.ID,
		WorkerName:     workerName,
		Result:         fmt.Sprintf("PROCESSED[%s] hash=%d", task.Data, len(task.Data)*task.ID),
		ProcessingTime: time.Since(start),
		CompletedAt:    time.Now(),
	}
	return result, nil
}

// =====================================================
// MAIN
// =====================================================

func main() {
	log.SetFlags(log.Ltime | log.Lmicroseconds)

	const (
		numWorkers = 4
		numTasks   = 20
	)

	log.Println("=== Data Processing System - Go ===")
	log.Printf("Config: %d workers, %d tasks\n", numWorkers, numTasks)

	// 1. Create buffered channel (acts as the shared task queue)
	taskCh := make(chan Task, numTasks)

	// 2. Create result store
	store := NewResultStore()

	// 3. Start worker goroutines
	var wg sync.WaitGroup
	for i := 1; i <= numWorkers; i++ {
		wg.Add(1)
		name := fmt.Sprintf("Worker-%d", i)
		go worker(name, taskCh, store, &wg)
	}

	// 4. Populate the channel with tasks
	dataTypes := []string{
		"SENSOR_READ", "LOG_PARSE", "IMAGE_RESIZE",
		"DB_QUERY", "API_CALL", "FILE_COMPRESS",
	}

	log.Println("Populating task channel...")
	for i := 1; i <= numTasks; i++ {
		taskCh <- Task{
			ID:       i,
			Data:     fmt.Sprintf("%s_%d", dataTypes[i%len(dataTypes)], i),
			Priority: (i % 3) + 1,
		}
	}

	// 5. Close channel — signals all workers no more tasks are coming
	close(taskCh)
	log.Println("Task channel closed. Waiting for workers...")

	// 6. Wait for all workers to finish
	wg.Wait()
	log.Println("All workers finished.")

	// 7. Print summary
	results := store.GetAll()
	log.Println("=== RESULTS SUMMARY ===")
	log.Printf("Total results stored: %d\n", len(results))
	log.Println("Sample results (first 5):")
	for i, r := range results {
		if i >= 5 {
			break
		}
		log.Printf("  %s\n", r.String())
	}

	// 8. Write results to file
	if err := store.WriteToFile("results.txt"); err != nil {
		log.Printf("[ERROR] writing results: %v\n", err)
	}

	log.Println("System shutdown complete.")
}