package llc.engine.res;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class Texture {

	public final String path;
	private int textureID;
	
	public Texture(String path) {
		this.path = path;
	}
	
	public void bind() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureID);
	}
	
	public void load() {
		try {
			BufferedImage image = ImageIO.read(new File(this.path));
			int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
			
			ByteBuffer buffer = BufferUtils.createByteBuffer(image.getHeight() * image.getWidth() * 4);
			boolean alpha = image.getColorModel().hasAlpha();
			
			for(int y = 0; y < image.getHeight(); y++) {
				for(int x = 0; x < image.getWidth(); x++) {
					int pixel = pixels[y * image.getWidth() + x];
					
					buffer.put((byte)((pixel >> 16) & 0xFF));
					buffer.put((byte)((pixel >> 8)  & 0xFF));
					buffer.put((byte)( pixel 		& 0xFF));
					if(alpha) buffer.put((byte)((pixel >> 24) & 0xFF));
					else buffer.put((byte)0xFF);
				}
			}
			
			buffer.flip();
		} catch(Exception e) {
		}
	}
	
}