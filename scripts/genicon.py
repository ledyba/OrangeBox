import cairo
import math

def createCairo(width, height):
	s = cairo.ImageSurface(cairo.FORMAT_ARGB32, int(width), int(height))
	c = cairo.Context(s)
	c.set_font_size(32.0)
	return s,c;

def measureSize(msg):
	_, c = createCairo(1,1)
	return c.text_extents(msg)
	# print (x_bearing, y_bearing, width, height, x_advance, y_advance)

def saveImage(msg, fname, color):
	(x_bearing, y_bearing, width, height, x_advance, y_advance) = measureSize(msg)
	s, c = createCairo(math.ceil(width)+3,math.ceil(height)+3)
	c.translate(-x_bearing + 1.5, -y_bearing + 1.5)
	c.text_path(msg);
	c.set_source_rgba(0,0,0,1);
	c.set_line_width(3)
	c.stroke_preserve();
	c.set_source_rgb(*color);
	c.fill();
	s.write_to_png(fname);

measureSize("test")

for x in range(0,101):
	s = str(x)
	saveImage(s, "img"+str(x)+"_red.png", (1,0,0));
	saveImage(s, "img"+str(x)+"_green.png", (0,1,0));
	saveImage(s, "img"+str(x)+"_white.png", (1,1,1));

