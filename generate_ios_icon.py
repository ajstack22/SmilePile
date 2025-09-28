#!/usr/bin/env python3

from PIL import Image, ImageDraw
import math

# Create a 1024x1024 image with white background
size = 1024
img = Image.new('RGBA', (size, size), (255, 255, 255, 255))
draw = ImageDraw.Draw(img)

# Scale factor (Android uses 256 viewport, we're drawing at 1024)
scale = size / 256

def draw_circle(x, y, radius, color):
    """Draw a filled circle"""
    x_scaled = x * scale
    y_scaled = y * scale
    r_scaled = radius * scale
    draw.ellipse([x_scaled - r_scaled, y_scaled - r_scaled,
                  x_scaled + r_scaled, y_scaled + r_scaled],
                 fill=color)

def draw_rotated_eyes(cx, cy, angle):
    """Draw eyes rotated at the specified angle around the circle center"""
    eye_color = '#212121'

    # Convert angle to radians
    angle_rad = math.radians(angle)

    # Original eye positions relative to center (before rotation)
    # Left eye at (-16, -12), Right eye at (16, -12)
    left_x = -16
    left_y = -12
    right_x = 16
    right_y = -12

    # Rotate the positions
    # Left eye
    rotated_left_x = cx + (left_x * math.cos(angle_rad) - left_y * math.sin(angle_rad))
    rotated_left_y = cy + (left_x * math.sin(angle_rad) + left_y * math.cos(angle_rad))

    # Right eye
    rotated_right_x = cx + (right_x * math.cos(angle_rad) - right_y * math.sin(angle_rad))
    rotated_right_y = cy + (right_x * math.sin(angle_rad) + right_y * math.cos(angle_rad))

    # Draw the eyes with radius 8 (matching Android)
    draw_circle(rotated_left_x, rotated_left_y, 8, eye_color)
    draw_circle(rotated_right_x, rotated_right_y, 8, eye_color)

# Draw the 5 overlapping circles WITH ROTATED EYES like ic_smilepile_logo.xml

# Bottom-left green circle at (80, 176) with eyes rotated -135 degrees
draw_circle(80, 176, 64, '#4CAF50')
draw_rotated_eyes(80, 176, -135)

# Bottom-right blue circle at (176, 176) with eyes rotated 135 degrees
draw_circle(176, 176, 64, '#2196F3')
draw_rotated_eyes(176, 176, 135)

# Top-right orange circle at (176, 80) with eyes rotated 45 degrees
draw_circle(176, 80, 64, '#FF6600')  # Using #FF6600 to match logo
draw_rotated_eyes(176, 80, 45)

# Top-left pink circle at (80, 80) with eyes rotated -45 degrees
draw_circle(80, 80, 64, '#E86082')
draw_rotated_eyes(80, 80, -45)

# Center golden smiley at (128, 128) - normal eyes (no rotation)
draw_circle(128, 128, 64, '#FFBF00')  # Using #FFBF00 to match logo

# Center eyes (not rotated)
draw_circle(112, 116, 8, '#212121')  # Left eye
draw_circle(144, 116, 8, '#212121')  # Right eye

# Draw smile on center circle
smile_color = '#212121'
smile_bbox = [
    (112 * scale) - 20 * scale,
    (136 * scale) - 16 * scale,
    (144 * scale) + 20 * scale,
    (136 * scale) + 32 * scale
]
draw.arc(smile_bbox, start=0, end=180, fill=smile_color, width=int(12 * scale))

# Save the image
img.save('/Users/adamstack/SmilePile/ios/SmilePile/Assets.xcassets/AppIcon.appiconset/icon-1024.png')
print("iOS app icon generated with ROTATED eyes on ALL circles!")