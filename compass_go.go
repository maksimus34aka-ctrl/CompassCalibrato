// compass_go.go — компас с калибровкой на Go (консоль)

package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"math"
	"math/rand"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	Offset     float64 `json:"offset"`
	Calibrated bool    `json:"calibrated"`
}

type App struct {
	angle      float64
	offset     float64
	calibrated bool
	simulating bool
	configFile string
}

func NewApp() *App {
	return &App{
		configFile: "compass_config.json",
	}
}

func (a *App) loadConfig() {
	data, err := ioutil.ReadFile(a.configFile)
	if err == nil {
		var cfg Config
		err = json.Unmarshal(data, &cfg)
		if err == nil {
			a.offset = cfg.Offset
			a.calibrated = cfg.Calibrated
		}
	}
}

func (a *App) saveConfig() {
	cfg := Config{Offset: a.offset, Calibrated: a.calibrated}
	data, _ := json.MarshalIndent(cfg, "", "  ")
	ioutil.WriteFile(a.configFile, data, 0644)
}

func (a *App) readAngle() {
	if !a.simulating {
		a.angle = math.Mod(a.angle + rand.Float64()*10 - 5, 360)
		if a.angle < 0 {
			a.angle += 360
		}
	}
}

func (a *App) calibrate() {
	fmt.Println("Калибровка... (имитация)")
	sum := 0.0
	for i := 0; i < 10; i++ {
		sum += rand.Float64() * 360
	}
	avg := sum / 10
	a.offset = math.Mod(0-avg, 360)
	if a.offset < 0 {
		a.offset += 360
	}
	a.calibrated = true
	fmt.Printf("Калибровка завершена. Смещение: %.2f°\n", a.offset)
	a.saveConfig()
}

func (a *App) reset() {
	a.offset = 0
	a.calibrated = false
	a.angle = 0
	fmt.Println("Сброшено")
}

func (a *App) toggleSimulation() {
	a.simulating = !a.simulating
	if a.simulating {
		fmt.Println("Симуляция включена")
	} else {
		fmt.Println("Симуляция выключена")
	}
}

func (a *App) manualInput(val float64) {
	a.angle = math.Mod(val, 360)
	if a.angle < 0 {
		a.angle += 360
	}
	fmt.Printf("Азимут установлен: %.1f°\n", a.angle)
}

func (a *App) display() {
	displayAngle := math.Mod(a.angle+a.offset, 360)
	if displayAngle < 0 {
		displayAngle += 360
	}
	dir := directionName(displayAngle)
	fmt.Printf("Азимут: %.1f° (%s)\n", displayAngle, dir)
}

func directionName(deg float64) string {
	dirs := []string{"С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"}
	idx := int(math.Mod(deg+22.5, 360)/45) % 8
	return dirs[idx]
}

func (a *App) interactive() {
	scanner := bufio.NewScanner(os.Stdin)
	fmt.Println("🧭 CompassCalibrator — Go Edition")
	fmt.Println("Команды: read, calibrate, reset, sim, set <deg>, info, exit")
	for {
		fmt.Print("> ")
		if !scanner.Scan() {
			break
		}
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		parts := strings.SplitN(line, " ", 2)
		cmd := parts[0]
		arg := ""
		if len(parts) > 1 {
			arg = parts[1]
		}
		switch cmd {
		case "read":
			a.readAngle()
			a.display()
		case "calibrate":
			a.calibrate()
			a.display()
		case "reset":
			a.reset()
			a.display()
		case "sim":
			a.toggleSimulation()
		case "set":
			if val, err := strconv.ParseFloat(arg, 64); err == nil {
				a.manualInput(val)
				a.display()
			} else {
				fmt.Println("Неверное число")
			}
		case "info":
			a.display()
		case "exit":
			a.saveConfig()
			fmt.Println("До свидания!")
			return
		default:
			fmt.Println("Неизвестная команда")
		}
	}
}

func main() {
	rand.Seed(time.Now().UnixNano())
	app := NewApp()
	app.loadConfig()
	app.interactive()
}
