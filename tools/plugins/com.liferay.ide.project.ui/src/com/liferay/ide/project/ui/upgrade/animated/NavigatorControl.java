
package com.liferay.ide.project.ui.upgrade.animated;

import com.liferay.ide.project.ui.upgrade.animated.AnimatedCanvas.Animator;
import com.liferay.ide.project.ui.upgrade.animated.GearAnimator.Page;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * @author Eike Stepper
 */
public class NavigatorControl extends AbstractCanvas
{
    private static Color WHITE;

    private static Color GRAY;

    private static Color DARK_GRAY;
    public NavigatorControl insance;
    private static final int DEFAULT_TIMER_INTERVAL = 10;
    private boolean overflow;
    private boolean oldShowOverlay;
    private int hover = NONE;
    private int oldHover = NONE;
    public static final int NONE = -1;
    private final Image[] backImages = new Image[2];
    private final Image[] nextImages = new Image[2];
    private int buttonR;
    private int answerY;
    private static final int BIG_FONT_PX = 48;
    public static final int PAGE_WIDTH = 400;
    public static final int PAGE_HEIGHT = 120;
    public static final int BORDER = 30;
    private Rectangle backBox;
    private Rectangle nextBox;
    private static final int EXIT = NONE - 1;
    private static final int BACK = EXIT - 1;
    private static final int NEXT = BACK - 1;    

    
    private UpgradePage[] pages;
    @Override
    protected void init()
    {
      super.init();

      bigFont = createFont(BIG_FONT_PX, PAGE_WIDTH, "");

      backImages[0] = loadImage("back.png");
      backImages[1] = loadImage("back_hover.png");

      nextImages[0] = loadImage("next.png");
      nextImages[1] = loadImage("next_hover.png");

      buttonR = nextImages[0].getBounds().height / 2;
      answerY = PAGE_HEIGHT + 4 * BORDER - buttonR;

    }

    private final Runnable runnable = new Runnable()
    {
        public void run()
        {
            doRun();
        }
    };
    
    protected boolean shouldShowOverlay()
    {
      return (System.currentTimeMillis() / 1000 & 1) == 1;
    }
    
    protected boolean advance()
    {
      boolean needsRedraw = false;

      if (overflow)
      {
        overflow = false;
        needsRedraw = true;
      }

      boolean showOverlay = shouldShowOverlay();
      
      if (showOverlay != oldShowOverlay)
      {
        oldShowOverlay = showOverlay;
        //updatePage();
        needsRedraw = true;
      }

      if (hover != oldHover)
      {
        needsRedraw = true;
      }

      return true;
    }
    protected synchronized void doRun()
    {
        if( isDisposed() )
        {
            return;
        }

        boolean needsRedraw = advance();

        if( needsRedraw )
        {
            redraw();
        }
        else
        {
            scheduleRun();
        }
    } 
    
    public NavigatorControl( Composite parent, int style, UpgradePage[] pages)
    {
        super( parent, style | SWT.DOUBLE_BUFFERED );
        
        this.pages = pages;
        
        Display display = getDisplay();
        
        setBackground( display.getSystemColor( SWT.COLOR_WHITE ) );
        
        WHITE = display.getSystemColor(SWT.COLOR_WHITE);
        GRAY = display.getSystemColor(SWT.COLOR_GRAY);
        DARK_GRAY = display.getSystemColor(SWT.COLOR_DARK_GRAY);
        
        insance = this;
        
        addFocusListener( new FocusListener()
        {
            public void focusGained( FocusEvent e )
            {
                redraw();
            }

            public void focusLost( FocusEvent e )
            {
                redraw();
            }
        } );

        addPaintListener( new PaintListener()
        {
            @Override
            public void paintControl( PaintEvent e )
            {
                Image buffer = new Image( getDisplay(), getBounds() );

                GC canvasGc = e.gc;
                //not blink
                GC bufferGC = new GC( buffer );

                bufferGC.setAdvanced( true );
                bufferGC.setBackground( canvasGc.getBackground() );
                bufferGC.fillRectangle( buffer.getBounds() );

                paint( bufferGC );
                
                canvasGc.drawImage( buffer, 0, 0 );

                bufferGC.dispose();
                buffer.dispose();
                
                scheduleRun();
            }
        } );


        addMouseTrackListener( new MouseTrackAdapter()
        {
            @Override
            public void mouseExit( MouseEvent e )
            {
                onMouseMove( Integer.MIN_VALUE, Integer.MIN_VALUE );
            }
        } );

        addMouseMoveListener( new MouseMoveListener()
        {
            public void mouseMove( MouseEvent e )
            {
                onMouseMove( e.x, e.y );
            }
        } );

        addMouseListener( new MouseAdapter()
        {
            @Override
            public void mouseDown( MouseEvent e )
            {
                //left button
                if( e.button == 1 )
                {
                    onMouseDown( e.x, e.y );
                }
            }
        } );
        
        init();
        
        scheduleRun();
    }
    
    private void scheduleRun()
    {
        getDisplay().timerExec( DEFAULT_TIMER_INTERVAL, runnable );
    }
    
    private UpgradePage getSelectedPage()
    {
        return pages[0];
    }

    protected boolean onMouseDown(int x, int y)
    {
      if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE)
      {
        GC gc = new GC(this);

        UpgradePage page = getSelectedPage();
        if (page != null)
        {
          if (page.backPage() && backBox != null && backBox.contains(x, y))
          {
            return true;
          }

          if (page.nextPage() && nextBox != null && nextBox.contains(x, y))
          {
            return true;
          }
        }
      }

      return false;
    }
    
    protected boolean onMouseMove(int x, int y)
    {
      if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE)
      {
        GC gc = new GC(this);

        UpgradePage page = getSelectedPage();
        if (page != null)
        {
          if (page.backPage() && backBox != null && backBox.contains(x, y))
          {
            hover = BACK;
            return true;
          }

          if (page.nextPage() && nextBox != null && nextBox.contains(x, y))
          {
            hover = NEXT;
            return true;
          }
        }
      }

      hover = NONE;
      return false;
    }

    private void paint( GC gc )
    {
        // System.out.println(System.currentTimeMillis()/1000);

        gc.setFont( getBaseFont() );
        gc.setLineWidth( 3 );
        gc.setAntialias( SWT.ON );

        backBox = drawImage( gc, backImages[hover == BACK ? 1 : 0], BORDER + buttonR, answerY );
        nextBox = drawImage( gc, nextImages[hover == NEXT ? 1 : 0], PAGE_WIDTH + BORDER - buttonR, answerY );

        oldHover = hover;
    }
    
}