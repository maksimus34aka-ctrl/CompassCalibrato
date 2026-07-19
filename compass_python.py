# compass_python.py — компас с калибровкой на Python (Tkinter)

import tkinter as tk
import math
import json
import os
import random
import time

class Compass:
    def __init__(self, root):
        self.root = root
        self.root.title("🧭 CompassCalibrator — Python")
        self.root.geometry("600x650")
        self.angle = 0          # текущий азимут в градусах
        self.offset = 0         # калибровочное смещение
        self.calibrated = False
        self.simulating = False
        self.sim_speed = 0.5    # градусов за обновление
        self.config_file = "compass_config.json"
        self.load_config()
        
        # Canvas для рисования компаса
        self.canvas = tk.Canvas(root, width=500, height=500, bg='white')
        self.canvas.pack(pady=10)
        self.draw_compass()
        
        # Панель управления
        control_frame = tk.Frame(root)
        control_frame.pack(pady=5)
        
        tk.Button(control_frame, text="Прочитать", command=self.read_angle).pack(side=tk.LEFT, padx=5)
        tk.Button(control_frame, text="Калибровка", command=self.calibrate).pack(side=tk.LEFT, padx=5)
        tk.Button(control_frame, text="Сброс", command=self.reset).pack(side=tk.LEFT, padx=5)
        tk.Button(control_frame, text="Симуляция", command=self.toggle_simulation).pack(side=tk.LEFT, padx=5)
        tk.Button(control_frame, text="Ввести азимут", command=self.manual_input).pack(side=tk.LEFT, padx=5)
        
        # Метка с информацией
        self.info_label = tk.Label(root, text="Азимут: 0° (С)", font=("Arial", 14))
        self.info_label.pack(pady=5)
        
        # Статус
        self.status = tk.Label(root, text="Готов", anchor=tk.W)
        self.status.pack(fill=tk.X, padx=10)
        
        # Таймер для симуляции
        self.timer = None
        self.update_compass()
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

    def draw_compass(self):
        self.canvas.delete("all")
        w = 500
        h = 500
        cx, cy = w//2, h//2
        r = 200
        # Внешний круг
        self.canvas.create_oval(cx-r, cy-r, cx+r, cy+r, outline='black', width=2)
        # Деления и подписи
        for deg in range(0, 360, 30):
            rad = math.radians(deg)
            x1 = cx + (r-20) * math.sin(rad)
            y1 = cy - (r-20) * math.cos(rad)
            x2 = cx + r * math.sin(rad)
            y2 = cy - r * math.cos(rad)
            self.canvas.create_line(x1, y1, x2, y2, width=2)
            if deg % 90 == 0:
                label = {0:'N', 90:'E', 180:'S', 270:'W'}[deg]
                xl = cx + (r-40) * math.sin(rad)
                yl = cy - (r-40) * math.cos(rad)
                self.canvas.create_text(xl, yl, text=label, font=("Arial", 16, "bold"))
        # Центральная точка
        self.canvas.create_oval(cx-5, cy-5, cx+5, cy+5, fill='black')
        # Стрелка (будет обновляться)
        self.update_arrow()

    def update_arrow(self):
        self.canvas.delete("arrow")
        w = 500; h = 500
        cx, cy = w//2, h//2
        angle_rad = math.radians(self.angle + self.offset)
        # Северный конец (красный)
        length = 150
        x1 = cx + 20 * math.sin(angle_rad)
        y1 = cy - 20 * math.cos(angle_rad)
        x2 = cx + length * math.sin(angle_rad)
        y2 = cy - length * math.cos(angle_rad)
        self.canvas.create_line(x1, y1, x2, y2, fill='red', width=6, tags="arrow")
        # Южный конец (синий)
        x3 = cx - 20 * math.sin(angle_rad)
        y3 = cy + 20 * math.cos(angle_rad)
        x4 = cx - length * 0.6 * math.sin(angle_rad)
        y4 = cy + length * 0.6 * math.cos(angle_rad)
        self.canvas.create_line(x3, y3, x4, y4, fill='blue', width=4, tags="arrow")
        # Наконечник стрелки (треугольник)
        self.canvas.create_polygon(
            x2, y2,
            x2 - 15 * math.cos(angle_rad - 0.4) + 15 * math.sin(angle_rad - 0.4),
            y2 - 15 * math.sin(angle_rad - 0.4) - 15 * math.cos(angle_rad - 0.4),
            x2 - 15 * math.cos(angle_rad + 0.4) - 15 * math.sin(angle_rad + 0.4),
            y2 - 15 * math.sin(angle_rad + 0.4) + 15 * math.cos(angle_rad + 0.4),
            fill='red', outline='red', tags="arrow"
        )

    def update_compass(self):
        # Обновление информации
        display_angle = (self.angle + self.offset) % 360
        direction = self.get_direction(display_angle)
        self.info_label.config(text=f"Азимут: {display_angle:.1f}° ({direction})")
        self.update_arrow()
        if self.simulating:
            self.angle = (self.angle + self.sim_speed) % 360
            self.status.config(text=f"Симуляция: {self.angle:.1f}°")
            self.timer = self.root.after(50, self.update_compass)
        else:
            self.timer = self.root.after(100, self.update_compass)

    def get_direction(self, deg):
        dirs = ["С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"]
        idx = int((deg + 22.5) // 45) % 8
        return dirs[idx]

    def read_angle(self):
        # В реальности здесь был бы вызов датчика, имитация
        if not self.simulating:
            # Генерируем случайное изменение для имитации реального компаса
            self.angle = (self.angle + random.uniform(-5, 5)) % 360
        self.status.config(text="Азимут прочитан")

    def calibrate(self):
        # Имитация калибровки: усреднение нескольких измерений
        self.status.config(text="Калибровка... (имитация)")
        samples = []
        for _ in range(10):
            # В реальности читаем датчик, здесь симуляция
            samples.append(random.uniform(0, 360))
        avg = sum(samples) / len(samples)
        # Предполагаем, что истинный север = 0, вычисляем смещение
        self.offset = (0 - avg) % 360
        self.calibrated = True
        self.status.config(text=f"Калибровка завершена. Смещение: {self.offset:.2f}°")
        self.save_config()

    def reset(self):
        self.offset = 0
        self.calibrated = False
        self.angle = 0
        self.status.config(text="Сброшено")

    def toggle_simulation(self):
        self.simulating = not self.simulating
        if self.simulating:
            self.status.config(text="Симуляция включена")
            if self.timer:
                self.root.after_cancel(self.timer)
            self.update_compass()
        else:
            self.status.config(text="Симуляция выключена")

    def manual_input(self):
        dialog = tk.Toplevel(self.root)
        dialog.title("Ввод азимута")
        tk.Label(dialog, text="Введите угол (0-360):").pack(pady=5)
        entry = tk.Entry(dialog)
        entry.pack(pady=5)
        def set_angle():
            try:
                val = float(entry.get()) % 360
                self.angle = val
                self.status.config(text=f"Азимут установлен: {val:.1f}°")
                dialog.destroy()
            except:
                pass
        tk.Button(dialog, text="Установить", command=set_angle).pack(pady=5)

    def load_config(self):
        if os.path.exists(self.config_file):
            with open(self.config_file, 'r') as f:
                data = json.load(f)
                self.offset = data.get('offset', 0)
                self.calibrated = data.get('calibrated', False)

    def save_config(self):
        data = {'offset': self.offset, 'calibrated': self.calibrated}
        with open(self.config_file, 'w') as f:
            json.dump(data, f)

    def on_close(self):
        self.save_config()
        if self.timer:
            self.root.after_cancel(self.timer)
        self.root.destroy()

if __name__ == "__main__":
    root = tk.Tk()
    app = Compass(root)
    root.mainloop()
