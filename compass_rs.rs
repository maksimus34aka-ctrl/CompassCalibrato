// compass_rs.rs — компас с калибровкой на Rust (консоль + termion)

use std::io::{self, Write, BufRead};
use std::fs;
use std::f64::consts::PI;
use rand::Rng;
use serde::{Deserialize, Serialize};
use serde_json;
use termion::{color, style};

#[derive(Serialize, Deserialize)]
struct Config {
    offset: f64,
    calibrated: bool,
}

struct App {
    angle: f64,
    offset: f64,
    calibrated: bool,
    simulating: bool,
    config_file: String,
    rng: rand::ThreadRng,
}

impl App {
    fn new() -> Self {
        App {
            angle: 0.0,
            offset: 0.0,
            calibrated: false,
            simulating: false,
            config_file: "compass_config.json".to_string(),
            rng: rand::thread_rng(),
        }
    }

    fn load_config(&mut self) {
        if let Ok(data) = fs::read_to_string(&self.config_file) {
            if let Ok(cfg) = serde_json::from_str::<Config>(&data) {
                self.offset = cfg.offset;
                self.calibrated = cfg.calibrated;
            }
        }
    }

    fn save_config(&self) {
        let cfg = Config { offset: self.offset, calibrated: self.calibrated };
        if let Ok(data) = serde_json::to_string_pretty(&cfg) {
            let _ = fs::write(&self.config_file, data);
        }
    }

    fn read_angle(&mut self) {
        if !self.simulating {
            let delta = self.rng.gen_range(-5.0..5.0);
            self.angle = (self.angle + delta) % 360.0;
            if self.angle < 0.0 { self.angle += 360.0; }
        }
    }

    fn calibrate(&mut self) {
        println!("Калибровка... (имитация)");
        let mut sum = 0.0;
        for _ in 0..10 {
            sum += self.rng.gen_range(0.0..360.0);
        }
        let avg = sum / 10.0;
        self.offset = (0.0 - avg) % 360.0;
        if self.offset < 0.0 { self.offset += 360.0; }
        self.calibrated = true;
        println!("Калибровка завершена. Смещение: {:.2}°", self.offset);
        self.save_config();
    }

    fn reset(&mut self) {
        self.offset = 0.0;
        self.calibrated = false;
        self.angle = 0.0;
        println!("Сброшено");
    }

    fn toggle_simulation(&mut self) {
        self.simulating = !self.simulating;
        if self.simulating {
            println!("Симуляция включена");
        } else {
            println!("Симуляция выключена");
        }
    }

    fn manual_input(&mut self, val: f64) {
        self.angle = val % 360.0;
        if self.angle < 0.0 { self.angle += 360.0; }
        println!("Азимут установлен: {:.1}°", self.angle);
    }

    fn display(&self) {
        let display_angle = (self.angle + self.offset) % 360.0;
        let display_angle = if display_angle < 0.0 { display_angle + 360.0 } else { display_angle };
        let dir = direction_name(display_angle);
        println!("{}Азимут: {:.1}° ({}){}", color::Fg(color::Green), display_angle, dir, style::Reset);
    }

    fn interactive(&mut self) {
        let stdin = io::stdin();
        let mut reader = stdin.lock();
        println!("{}🧭 CompassCalibrator — Rust Edition{}", color::Fg(color::Cyan), style::Reset);
        println!("Команды: read, calibrate, reset, sim, set <deg>, info, exit");
        loop {
            print!("{}> {} ", color::Fg(color::Yellow), style::Reset);
            io::stdout().flush().unwrap();
            let mut line = String::new();
            if reader.read_line(&mut line).is_err() { break; }
            let line = line.trim();
            if line.is_empty() { continue; }
            let parts: Vec<&str> = line.splitn(2, ' ').collect();
            let cmd = parts[0];
            let arg = if parts.len() > 1 { parts[1] } else { "" };
            match cmd {
                "read" => {
                    self.read_angle();
                    self.display();
                }
                "calibrate" => {
                    self.calibrate();
                    self.display();
                }
                "reset" => {
                    self.reset();
                    self.display();
                }
                "sim" => self.toggle_simulation(),
                "set" => {
                    if let Ok(val) = arg.parse::<f64>() {
                        self.manual_input(val);
                        self.display();
                    } else {
                        println!("Неверное число");
                    }
                }
                "info" => self.display(),
                "exit" => {
                    self.save_config();
                    println!("До свидания!");
                    break;
                }
                _ => println!("Неизвестная команда"),
            }
        }
    }
}

fn direction_name(deg: f64) -> String {
    let dirs = ["С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"];
    let idx = ((deg + 22.5) / 45.0) as usize % 8;
    dirs[idx].to_string()
}

fn main() {
    let mut app = App::new();
    app.load_config();
    app.interactive();
}
