package llc.engine.gui;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import llc.engine.GUIRenderer;
import llc.engine.res.Texture;
import llc.util.RenderUtil;

import org.lwjgl.input.Mouse;

public abstract class GUIHotkeyButton extends GUIElement {

	private boolean isHover = false;
	private boolean isClicked = false;
	private boolean wasClicked = false;
	
	private static final Texture button;
	private static final Texture buttonHover;
	private static final Texture buttonDown;
	
	static {
		button = new Texture("res/gui/img_btn.png");
		buttonHover = new Texture("res/gui/img_btn_hover.png");
		buttonDown = new Texture("res/gui/img_btn_down.png");
	}
	
	public GUIHotkeyButton(GUI gui, float posX, float posY, float width, float height) {
		super(gui, posX, posY, width, height);
	}

	@Override
	public void update(int x, int y) {
		if(this.isClicked && !this.wasClicked) this.onClick(x, y);
		this.wasClicked = this.isClicked;
		
		this.isHover = x >= this.posX && x <= this.posX + this.width && y >= this.posY && y <= this.posY + this.height;
		this.isClicked = this.isHover && Mouse.isButtonDown(0);
	}

	@Override
	public void render(GUIRenderer renderer, int x, int y) {
		glEnable(GL_TEXTURE_2D);
		RenderUtil.clearColor();
		
		if(this.isClicked) buttonDown.bind();
		else if(this.isHover) buttonHover.bind();
		else button.bind();
		
		RenderUtil.drawTexturedQuad(this.posX, this.posY, this.width, this.height);
		glDisable(GL_TEXTURE_2D);
	}

	/**
	 * Gets triggered when the button gets clicked
	 */
	public abstract void onClick(int x, int y);
	
}
