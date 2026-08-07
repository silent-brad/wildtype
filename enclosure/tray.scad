// Wildtype — Single-piece Pi + Webcam Tray
// Print time: ~30 min at 0.3mm layer height, 20% infill
// Material: PLA, ~15g

$fn = 60;

// Dimensions (mm)
pi_w = 85;
pi_d = 56;
pi_h = 17;
cam_w = 35;
cam_d = 30;
cam_h = 25;
wall = 2;
base = 2;
spacing = 5;
total_w = pi_w + wall * 2;
total_d = pi_d + cam_d + spacing + wall * 2;
total_h = max(pi_h, cam_h) + wall;

module tray() {
    difference() {
        // Outer shell
        cube([total_w, total_d, total_h]);

        // Pi cavity
        translate([wall, wall, base])
            cube([pi_w, pi_d, pi_h + wall]);

        // Camera cavity
        translate([wall, pi_d + spacing + wall, base])
            cube([cam_w, cam_d, cam_h + wall]);

        // Cable slot (Pi side)
        translate([wall + 10, wall + pi_d, base])
            cube([20, wall + 2, 8]);

        // Camera lens hole
        translate([wall + cam_w / 2 - 8, pi_d + spacing + wall + cam_d, base + 5])
            cube([16, wall + 2, 12]);

        // Mounting holes (Pi M2.5)
        for (x = [25, 58], y = [18, 49]) {
            translate([x, y, 0])
                cylinder(h = base + 1, d = 3);
        }
    }
}

tray();
