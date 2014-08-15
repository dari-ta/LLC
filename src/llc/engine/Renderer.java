package llc.engine;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import llc.engine.res.Model;
import llc.engine.res.Model.Face;
import llc.engine.res.Model.Material;
import llc.engine.res.ObjLoader;
import llc.engine.res.Program;
import llc.engine.res.Shader;
import llc.engine.res.Texture;
import llc.entity.Entity;
import llc.entity.EntityBuildingBase;
import llc.entity.EntityWarrior;
import llc.entity.EntityWorker;
import llc.logic.Cell;
import llc.logic.GameState;
import llc.util.RenderUtil;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL20;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

public class Renderer {
	
	private static final float sandRegion = 0.15f;
	private static final float terrainScale = 5;
	
	private Texture loadingScreen;
	
	private Texture waterTexture;
	private Texture grassTexture;
	private Texture sandTexture;
	
	private Triangle[][][] triangles;
	
	private Model baseModel;
	private int baseId;
	private Model minerModel;
	private int minerId;
	private Model warriorModel;
	private int warriorId;
	
	private int frameBufferId;
	
	private Program shaderProg;
	
	List<GradientPoint> colors = new ArrayList<GradientPoint>();

	public Renderer() {
		System.out.println("OpenGL version: " + glGetString(GL_VERSION));
		
		// OpenGL setup
		glClearColor(0F, 0F, 0F, 1F);

		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);

		// loading screen
		this.loadingScreen = new Texture("res/gui/logo.png");
		this.drawLoadingScreen(Display.getWidth(), Display.getHeight());
		
		// textures
		waterTexture = new Texture("res/texture/water.png");
		grassTexture = new Texture("res/texture/grass.png");
		sandTexture = new Texture("res/texture/sand.png");
		
		// meshes
		try {
			baseModel = ObjLoader.loadTexturedModel(new File("res/entity/base/Medieval_House.obj"));
			baseId = ObjLoader.createTexturedDisplayList(baseModel);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			warriorModel = ObjLoader.loadTexturedModel(new File("res/entity/moveable/warrior/knight.obj"));
			warriorId = ObjLoader.createTexturedDisplayList(warriorModel);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			minerModel = ObjLoader.loadTexturedModel(new File("res/entity/moveable/miner/landlord.obj"));
			minerId = ObjLoader.createTexturedDisplayList(minerModel);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// gradient
		colors.add(new GradientPoint(-1f, new Vector3f(0f, 0f, 1f)));
		colors.add(new GradientPoint(-0.25f,new Vector3f(1f, 1f, 1f)));
		colors.add(new GradientPoint(0.15f,new Vector3f(1f, 1f, 1f)));
		colors.add(new GradientPoint(0.25f,new Vector3f(0.5f, 1f, 1f)));
		colors.add(new GradientPoint(0.5f,new Vector3f(1f, 1f, 1f)));
		
		// shader
		this.shaderProg = new Program();
		this.shaderProg.addShader(new Shader("res/shaders/test.vert", Shader.vertexShader));
		this.shaderProg.addShader(new Shader("res/shaders/test.frag", Shader.fragmentShader));
		this.shaderProg.validate();
		
		// FBO for render to texture, from: http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-14-render-to-texture/
		frameBufferId = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);
		
		int renderedTextureId  = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, renderedTextureId);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 1024, 768, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer)null); // empty
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST); // have to use nearest when rendering to
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		
		int depthRenderBufferId = glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBufferId);
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, 1024, 768); // must be the same as the rendered texture's size
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderBufferId);
		
		glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, renderedTextureId, 0);
		 
		GL20.glDrawBuffers(GL_COLOR_ATTACHMENT0);
		
		if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
			System.err.println("Error: custom framebuffer is not complete");
		
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	/**
	 * Handles the OpenGL-Part when the Display was resized
	 */
	public void handleDisplayResize(int width, int height) {
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(45, (float) width / (float) height, 0.1F, 100F);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glViewport(0, 0, width, height);
	}

	/**
	 * Is called each Display-Tick to render the game
	 */
	public void render(Camera camera, GameState gameState) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		// Apply camera transformation
		glLoadIdentity();
		gluLookAt(camera.pos.x, camera.pos.y, camera.pos.z, camera.pos.x
				+ camera.viewDir.x, camera.pos.y + camera.viewDir.y,
				camera.pos.z + camera.viewDir.z, camera.up.x, camera.up.y,
				camera.up.z);

		int width = gameState.getGrid().getWidth();
		int height = gameState.getGrid().getHeigth();
		
		drawGridTexture(gameState, width, height);
		
		drawCoordinateSystem();
		drawGrid(gameState, width, height);
		drawHoveredAndSelectedCells(gameState);
		drawEntities(gameState, width, height);
		drawWaterSurface(width, height);
	}
	
	private void drawGridTexture(GameState state, int width, int height) {
		glPushAttrib(GL_VIEWPORT_BIT);
		glViewport(0,0,1024,768);
		glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);
		
		
		drawGrid(state, width, height);
		
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		glPopAttrib();
	}

	private void drawWaterSurface(int width, int height) {
		
		glEnable(GL_TEXTURE_2D);
		waterTexture.bind();
		glColor4f(1,  1,  1, 0.8f);
		float cellCount = 10;
		glBegin(GL_TRIANGLES);
		glTexCoord2d(0, cellCount);
		glVertex3f(0, 0, 0);
		glTexCoord2d(cellCount, cellCount);
		glVertex3f(width, 0, 0);
		glTexCoord2d(0, 0);
		glVertex3f(0, height, 0);

		glTexCoord2d(cellCount, cellCount);
		glVertex3f(width, 0, 0);
		glTexCoord2d(cellCount, 0);
		glVertex3f(width, height, 0);
		glTexCoord2d(0, 0);
		glVertex3f(0, height, 0);
		glEnd();
		
		glDisable(GL_TEXTURE_2D);
	}
	
	public void generateGridGeometry (GameState state)
	{
		int width = state.getGrid().getWidth();
		int height = state.getGrid().getHeigth();
		Cell[][] cells = state.getGrid().getCells();
		triangles = new Triangle[height][width][2];
		for (int y = 0; y < height; y++) 
		{
			for (int x = 0; x < width; x++) 
			{
				//Generating heigth coordinates
				float currentHeight = cells[y][x].height;
				float[][] heights = new float [3][3];
				heights[0][0] = y > 0 && x > 0 ? cells[y - 1][x - 1].height : currentHeight;
				heights[0][1] = y > 0 ? cells[y - 1][x].height : currentHeight;
				heights[0][2] = y > 0 && x < width - 1 ? cells[y - 1][x + 1].height : currentHeight;
				heights[1][0] = x > 0 ? cells[y][x - 1].height : currentHeight;
				heights[1][1] = currentHeight;
				heights[1][2] = x < width - 1 ? cells[y][x + 1].height : currentHeight;
				heights[2][0] = y < height - 1 && x > 0 ? cells[y + 1][x - 1].height : currentHeight;
				heights[2][1] = y < height - 1 ? cells[y + 1][x].height : currentHeight;
				heights[2][2] = y < height -1 && x < width - 1 ? cells[y + 1][x + 1].height : currentHeight;
				
				float topRightHeight = (heights[0][1] + heights[0][2] + heights[1][1] + heights[1][2]) / 4f;
				float topLeftHeight = (heights[0][0] + heights[0][1] + heights[1][0] + heights[1][1]) / 4f;
				float bottomRightHeight = (heights[1][1] + heights[1][2] + heights[2][1] + heights[2][2]) / 4f;
				float bottomLeftHeight = (heights[1][0] + heights[1][1] + heights[2][1] + heights[2][0]) / 4f;
				
				topRightHeight = topRightHeight * terrainScale;
				topLeftHeight = topLeftHeight * terrainScale;
				bottomRightHeight = bottomRightHeight * terrainScale;
				bottomLeftHeight = bottomLeftHeight * terrainScale;
				
				Vector3f topLeft = new Vector3f(x, y,  topLeftHeight);
				Vector3f topRight = new Vector3f(x + 1, y, topRightHeight);
				Vector3f bottomLeft = new Vector3f(x, y + 1, bottomLeftHeight);
				Vector3f bottomRight = new Vector3f(x + 1, y + 1, bottomRightHeight);
				
				Vector3f topLeftNormal = calcNormal(x, y, heights[0][0], heights[0][1], heights[1][0], heights[1][1]);
				Vector3f topRightNormal = calcNormal(x + 1, y, heights[0][1], heights[0][2], heights[1][1], heights[1][2]);
				Vector3f bottomLeftNormal = calcNormal(x, y + 1, heights[1][0], heights[1][1], heights[2][1], heights[2][0]);
				Vector3f bottomRightNormal = calcNormal(x + 1, y + 1, heights[1][0], heights[1][1], heights[2][1], heights[2][0]);
				
				triangles[y][x][0] = new Triangle(
						new Vertex(
								topLeft,
								topLeftNormal,
								new Vector2f(0, 1)
								),
						new Vertex(
								topRight,
								topRightNormal,
								new Vector2f(1, 1)
								),
						new Vertex(
								bottomLeft,
								bottomLeftNormal,
								new Vector2f(0, 0)
								)
						);
				triangles[y][x][1] = new Triangle(
						new Vertex(
								topRight,
								topRightNormal,
								new Vector2f(1, 1)
								),
						new Vertex(
								bottomRight,
								bottomRightNormal,
								new Vector2f(1, 0)
								),
						new Vertex(
								bottomLeft,
								bottomLeftNormal,
								new Vector2f(0, 0)
								)
						);
			}
		}
	}
	
	private void drawHoveredAndSelectedCells(GameState state)
	{
		// highlight hovered cell
		glEnable(GL_TEXTURE_2D);
		this.shaderProg.bind();
		if(state.hoveredCell != null && state.selectedCell == state.hoveredCell)
		{
			glColor3f(1, 0.5f, 1);
			drawCell(state.hoveredCell, state.hoveredCell.y, state.hoveredCell.x, false);
			//System.out.println("hovered and selected" + state.hoveredCell.x + " " + state.hoveredCell.y);
		}
		else if (state.hoveredCell != null)
		{
			glColor3f(1, 0.5f, 0.5f);
			drawCell(state.hoveredCell, state.hoveredCell.y, state.hoveredCell.x, false);
			//System.out.println("hovered " + state.hoveredCell.x + " " + state.hoveredCell.y);
		}
		else if (state.selectedCell != null)
		{
			glColor3f(0.5f, 0.5f, 1f);
			drawCell(state.selectedCell, state.selectedCell.y, state.selectedCell.x, false);
			//System.out.println("selected " + state.selectedCell.x + " " + state.selectedCell.y);
		}
		RenderUtil.unbindShader();
		glDisable(GL_TEXTURE_2D);
	}
	
	private void bindModelTexture(Model m) {
		for(Face f : m.getFaces()) {
			Material mat = f.getMaterial();
			if(mat != null && mat.texture != null) {
				mat.texture.bind();
				return;
			}
		}
	}
	
	private void drawEntities(GameState state, int width, int height) {
		this.shaderProg.bind();
		
		Cell[][] cells = state.getGrid().getCells();
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Cell c = cells[y][x];
				Entity e = c.getEntity();
				
				if(e != null) {
					glPushMatrix();
					glTranslatef(x + 0.5F, y + 0.5F, c.height + 1);
					glColor3f(1, 1, 1);
					if(e instanceof EntityBuildingBase) {
						bindModelTexture(baseModel);
						glCallList(this.baseId);
					} else if(e instanceof EntityWarrior) {
						bindModelTexture(warriorModel);
						glCallList(this.warriorId);
					} else if(e instanceof EntityWorker) {
						bindModelTexture(minerModel);
						glCallList(this.minerId);
					}
					glPopMatrix();
				}
			}
		}
		
		RenderUtil.unbindShader();
	}

	private void drawGrid(GameState state, int width, int height) {
		glEnable(GL_TEXTURE_2D);
		this.shaderProg.bind();
		glColor3f(1, 1, 1);
		
		Cell[][] cells = state.getGrid().getCells();
		for (int y = 0; y < height; y++) 
		{
			for (int x = 0; x < width; x++) 
			{
				Cell c = cells[y][x];
				drawCell(c, y, x, true);
			}
		}
		
		RenderUtil.unbindShader();
		glDisable(GL_TEXTURE_2D);
	}

	private void drawCell(Cell c, int y, int x, boolean allowColor) {
		float h = c.getHeight();
		// Render terrain texture
		if (h < -sandRegion)
			waterTexture.bind();
		else if (h > sandRegion)
			grassTexture.bind();
		else 
			sandTexture.bind();
		
		Triangle[] ts = triangles[y][x];
		glBegin(GL_TRIANGLES);
		
		for (int t = 0; t < 2; t++) {
			for (int i = 0; i < 3; i++) {
				if(allowColor) {
					Vector3f color = getTerrainColorFromHeight(ts[t].vertices[i].position.z / terrainScale);
					glColor3f(color.x, color.y, color.z);
				}
				
				glTexCoord2d(ts[t].vertices[i].texCoord.x, ts[t].vertices[i].texCoord.y);
				glNormal3f(ts[t].vertices[i].normal.x, ts[t].vertices[i].normal.y, ts[t].vertices[i].normal.z);
				glVertex3f(ts[t].vertices[i].position.x, ts[t].vertices[i].position.y, ts[t].vertices[i].position.z);
			}
		}

		glEnd();
	}

	private Vector3f calcNormal(int x, int y, float topLeftHeight, float topRightHeight, float bottomLeftHeight, float bottomRightHeight) {
		Vector3f topLeft = new Vector3f(x, y,  topLeftHeight);
		Vector3f topRight = new Vector3f(x + 1, y, topRightHeight);
		Vector3f bottomLeft = new Vector3f(x, y + 1, bottomLeftHeight);
		Vector3f bottomRight = new Vector3f(x + 1, y + 1, bottomRightHeight);
		Vector3f diagonal1 = Vector3f.sub(topLeft, bottomRight, null);
		Vector3f diagonal2 = Vector3f.sub(topRight, bottomLeft, null);
		Vector3f normal = Vector3f.cross(diagonal1, diagonal2, null).normalise(null);
		return normal;
	}

	private void drawCoordinateSystem() {
		glDisable(GL_TEXTURE_2D); // someone leaves textures enabled, fix this
		
		// Draw coordinate system
		glBegin(GL_LINES);
		glColor3f(1, 0, 0);
		glVertex3f(0, 0, 0);
		glVertex3f(10, 0, 0);

		glColor3f(0, 1, 0);
		glVertex3f(0, 0, 0);
		glVertex3f(0, 10, 0);

		glColor3f(0, 0, 1);
		glVertex3f(0, 0, 0);
		glVertex3f(0, 0, 10);
		glEnd();
	}
	
	private void drawLoadingScreen(int width, int height) {
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, width, height, 0, -1, 1);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glViewport(0, 0, width, height);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		glEnable(GL_TEXTURE_2D);
		this.loadingScreen.bind();
		RenderUtil.drawTexturedQuad(0, 0, width, height);
		glDisable(GL_TEXTURE_2D);
		
		Display.update();
	}
	
	private class GradientPoint {
		public float height;
		public Vector3f color;
		
		public GradientPoint(float height, Vector3f color) {
			this.height = height;
			this.color = color;
		}
	}
		
	
	private Vector3f getTerrainColorFromHeight(float height) {
		
		// find gradient colors around height
		int upper;
		for(upper = 0; upper < colors.size(); upper++) {
			if(colors.get(upper).height > height)
				break;
		}
		
		GradientPoint upperColor = colors.get(Math.min(upper, colors.size() - 1));
		GradientPoint lowerColor = colors.get(Math.max(upper - 1, 0));
		
		if(upperColor != lowerColor) {
			float lowerPart = (height - lowerColor.height) / (upperColor.height - lowerColor.height);
		
			Vector3f color = new Vector3f(
				lowerColor.color.x * (1 - lowerPart) + upperColor.color.x * lowerPart,
				lowerColor.color.y * (1 - lowerPart) + upperColor.color.y * lowerPart,
				lowerColor.color.z * (1 - lowerPart) + upperColor.color.z * lowerPart);
			return color;
		}
		else return upperColor.color;
	}
	
}
