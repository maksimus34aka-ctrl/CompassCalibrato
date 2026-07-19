// compass_js.js — компас с калибровкой на JavaScript (Node.js + readline)

const fs = require('fs');
const readline = require('readline');

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: '> '
});

class App {
    constructor() {
        this.angle = 0;
        this.offset = 0;
        this.calibrated = false;
        this.simulating = false;
        this.configFile = 'compass_config.json';
        this.loadConfig();
    }

    loadConfig() {
        try {
            const data = fs.readFileSync(this.configFile, 'utf8');
            const cfg = JSON.parse(data);
            this.offset = cfg.offset || 0;
            this.calibrated = cfg.calibrated || false;
        } catch (e) {}
    }

    saveConfig() {
        const cfg = { offset: this.offset, calibrated: this.calibrated };
        fs.writeFileSync(this.configFile, JSON.stringify(cfg, null, 2));
    }

    readAngle() {
        if (!this.simulating) {
            this.angle = (this.angle + (Math.random() * 10 - 5)) % 360;
            if (this.angle < 0) this.angle += 360;
        }
    }

    calibrate() {
        console.log('Калибровка... (имитация)');
        let sum = 0;
        for (let i = 0; i < 10; i++) {
            sum += Math.random() * 360;
        }
        const avg = sum / 10;
        this.offset = (0 - avg) % 360;
        if (this.offset < 0) this.offset += 360;
        this.calibrated = true;
        console.log(`Калибровка завершена. Смещение: ${this.offset.toFixed(2)}°`);
        this.saveConfig();
    }

    reset() {
        this.offset = 0;
        this.calibrated = false;
        this.angle = 0;
        console.log('Сброшено');
    }

    toggleSimulation() {
        this.simulating = !this.simulating;
        console.log(`Симуляция ${this.simulating ? 'включена' : 'выключена'}`);
        if (this.simulating) {
            this.simInterval = setInterval(() => {
                this.angle = (this.angle + 0.5) % 360;
                this.display();
            }, 50);
        } else {
            clearInterval(this.simInterval);
        }
    }

    manualInput(val) {
        this.angle = val % 360;
        if (this.angle < 0) this.angle += 360;
        console.log(`Азимут установлен: ${this.angle.toFixed(1)}°`);
    }

    display() {
        const displayAngle = (this.angle + this.offset) % 360;
        const finalAngle = displayAngle < 0 ? displayAngle + 360 : displayAngle;
        const dir = this.directionName(finalAngle);
        console.log(`Азимут: ${finalAngle.toFixed(1)}° (${dir})`);
    }

    directionName(deg) {
        const dirs = ['С', 'СВ', 'В', 'ЮВ', 'Ю', 'ЮЗ', 'З', 'СЗ'];
        const idx = Math.floor((deg + 22.5) / 45) % 8;
        return dirs[idx];
    }

    interactive() {
        console.log('🧭 CompassCalibrator — JavaScript Edition');
        console.log('Команды: read, calibrate, reset, sim, set <deg>, info, exit');
        rl.prompt();

        rl.on('line', (line) => {
            const parts = line.trim().split(' ');
            const cmd = parts[0];
            const arg = parts.slice(1).join(' ');
            switch (cmd) {
                case 'read':
                    this.readAngle();
                    this.display();
                    break;
                case 'calibrate':
                    this.calibrate();
                    this.display();
                    break;
                case 'reset':
                    this.reset();
                    this.display();
                    break;
                case 'sim':
                    this.toggleSimulation();
                    break;
                case 'set':
                    const val = parseFloat(arg);
                    if (!isNaN(val)) {
                        this.manualInput(val);
                        this.display();
                    } else {
                        console.log('Неверное число');
                    }
                    break;
                case 'info':
                    this.display();
                    break;
                case 'exit':
                    this.saveConfig();
                    console.log('До свидания!');
                    rl.close();
                    return;
                default:
                    console.log('Неизвестная команда');
            }
            rl.prompt();
        }).on('close', () => {
            process.exit(0);
        });
    }
}

const app = new App();
app.interactive();
