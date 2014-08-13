package llc.input;

import java.util.*;
import org.lwjgl.util.vector.Vector3f;

import llc.LLC;
import llc.engine.Camera;


public class Input 
{
	private Camera Cam;
	
	public int scrollFrameBorder = 30;
 
	private LLC LLC_ref;
	
	public Input(LLC reference)
	{
		LLC_ref = reference;
		
	}
	public void mouseClick(int x, int y)
	{		
		
		Vector3f clickPos = new Vector3f(x, y, 1) ;
		
		clickPos.x = x;
		clickPos.y = y;
		clickPos.z = 1; // z of projection-plane is 1 ?
		
		float t = (- Cam.pos.z)/ (clickPos.z - Cam.pos.z); // z of the grid is 0
		
		int cell_x = (int) ((int) Cam.pos.x + t * (clickPos.x - Cam.pos.x));
		int cell_y = (int) ((int) Cam.pos.y + t * (clickPos.y - Cam.pos.y));
		
		FireCellClickedEvent(cell_x, cell_y);
		
		
	}
	
	public void mousePos(int x, int y)
	{
	
		int h = LLC_ref.height;
		int w = LLC_ref.width;
		
		if (x < scrollFrameBorder) FireScrollEvent(Direction.left);
		if (x > w - scrollFrameBorder) FireScrollEvent(Direction.right);

		if (y < scrollFrameBorder) FireScrollEvent(Direction.up);
		if (y > h - scrollFrameBorder) FireScrollEvent(Direction.down);

	}
	
	// ------------- Interface for the Listener -------------
	public interface LogicListener
	{
		public void onScroll(Direction d);
		public void onCellClicked(int cell_x, int cell_y);
	}
	
	public enum Direction
	{
		left,
		right,
		down,
		up;
	}
	// -------------------------------------------------------
	
	List<LogicListener> listeners = new ArrayList<LogicListener>();
	
	
	// ------------ Function to add yourself as Listener
	public void addFireListener(LogicListener toAdd){ listeners.add(toAdd); }

    public void FireScrollEvent(Direction d) 
    {
        for (LogicListener hl : listeners) hl.onScroll(d);
    }
    
    public void FireCellClickedEvent(int cell_x, int cell_y) 
    {
        for (LogicListener hl : listeners) hl.onCellClicked(cell_x,cell_y);
    }
	
	
}