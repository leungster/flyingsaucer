
/* 
 * {{{ header & license 
 * Copyright (c) 2004 Joshua Marinacci 
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation; either version 2.1 
 * of the License, or (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the 
 * GNU Lesser General Public License for more details. 
 * 
 * You should have received a copy of the GNU Lesser General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA. 
 * }}} 
 */

package org.xhtmlrenderer.layout;

import org.w3c.dom.*;
import org.xhtmlrenderer.render.*;
import org.xhtmlrenderer.util.u;
import org.xhtmlrenderer.util.*;
import java.awt.Rectangle;
import java.awt.Font;



public class LineBreaker {
public static InlineBox generateMultilineBreak(Context c, Node node, int start, String text,
    InlineBox prev, InlineBox prev_align, int avail) {
    //u.p("normal breaking");
    // calc end index to most words that will fit
    int end = start;
    int dbcount = 0;
    while(true) {
        dbcount++;
        //u.off();
        u.on();
        if(dbcount > 50) {
            u.on();
        }
        if(dbcount>100) {
            u.on();
            u.p("db 2 hit");
            u.p("text = " + text);
            u.p("end = " + end);
            throw new InfiniteLoopError("Caught a potential infinite loop in the LineBreaker");
        }

        //u.p("end = " + end);
        int next_space = text.indexOf(" ",end);
        if(next_space == -1) { next_space = text.length(); }
        //u.p("next space = " + next_space);
        
        Font font = FontUtil.getFont(c,node);

        int len2 = FontUtil.len(c,node,text.substring(start,next_space),font);
        //u.p("len2 = " + len2 + " avail = " + avail);
        // if this won't fit, then break and use the previous span
        if(len2 > avail) {
            InlineBox box = newBox(c, node, start, end, prev, text, prev_align, font);
            //u.p("normal break returning span: " + box);
            return box;
        }
        // if this will fit but we are at the end then break and use current span
        if(next_space == text.length()) {
            InlineBox box = newBox(c, node, start, next_space, prev, text, prev_align, font);
            //u.p("normal break returning curr span: " + box);
            return box;
        }
        // skip over the space
        end = next_space + 1;
    }
}

public static boolean canFitOnLine(Context c, Node node, int start, String text, int avail, Font font) {
    // if the rest of text can fit on the current line
    // if length of remaining text < available width
    //u.p("avail = " + avail + " len = " + FontUtil.len(c,node,text.substring(start)));
    if(FontUtil.len(c,node,text.substring(start),font ) < avail) {
        return true;
    } else {
        return false;
    }
}
public static InlineBox generateRestOfTextNodeInlineBox(Context c, Node node, int start, String text,
    InlineBox prev, InlineBox prev_align, Font font) {
        InlineBox box = newBox(c,node,start,text.length(),prev,text, prev_align, font);
        // turn off breaking since more might fit on this line
        box.break_after = false;
        //u.p("fits on line returning : " + box);
        return box;
}

public static boolean isUnbreakableLine(Context c, Node node, int start, String text, int avail, Font font) {
    int first_word_index = text.indexOf(" ",start);
    if(first_word_index == -1) {
        first_word_index = text.length();
    }
    String first_word = text.substring(start,first_word_index);
    first_word = first_word.trim();
    if(avail < FontUtil.len(c, node, first_word, font)) {
        return true;
    } else {
        return false;
    }
}


public static InlineBox generateUnbreakableInlineBox(Context c, Node node, int start, String text,  InlineBox prev, InlineBox prev_align, Font font) {
    int first_word_index = text.indexOf(" ",start);
    if(first_word_index == -1) {
        first_word_index = text.length();
    }
    String first_word = text.substring(start,first_word_index);
    first_word = first_word.trim();
    InlineBox box = newBox(c, node, start, first_word_index, prev, text, prev_align,font);
    // move back to the left margin since this is on it's own line
    box.x = 0;
    box.break_before = true;
    //u.p("unbreakable long word returning: " + box);
    box.break_after = true;
    return box;
}

public static boolean isWhitespace(Context c, Element containing_block) {
    String white_space = c.css.getStringProperty(containing_block,"white-space");
    // if doing preformatted whitespace
    if(white_space!=null && white_space.equals("pre")) {
        return true;
    } else {
        return false;
    }
}

public static InlineBox generateWhitespaceInlineBox(Context c, Node node, int start,
    InlineBox prev, String text, InlineBox prev_align, Font font) {
        //u.p("preformatted text");
        int cr_index = text.indexOf("\n",start+1);
        //u.p("cr_index = " + cr_index);
        if(cr_index == -1) {
            cr_index = text.length();
        }
        InlineBox box = newBox(c,node,start,cr_index,prev,text, prev_align, font);
        return box;
}

public static InlineBox generateBreakInlineBox(Node node) {
    InlineBox box = new InlineBox();
    box.node = node;
    box.width = 0;
    box.height = 0;
    box.break_after = true;
    box.x = 0;
    box.y = 0;
    box.is_break = true;
    return box;
}

// change this to use the existing block instead of a new one
public static InlineBox generateFloatedBlockInlineBox(Context c, Node node, int avail, InlineBox prev, String text, InlineBox prev_align, Font font) {
    Layout layout = LayoutFactory.getLayout(node);
    Rectangle oe = c.getExtents();
    c.setExtents(new Rectangle(oe));
    BlockBox block = (BlockBox) layout.layout(c,(Element)node);
    Rectangle bounds = new Rectangle(block.x,block.y,block.width,block.height);
    c.setExtents(oe);
    InlineBox box = newBox(c,node,0,0,prev,text,bounds,prev_align, font);
    box.sub_block = block;
    box.width = bounds.width;
    box.height = bounds.height;
    box.break_after = false;
    if(box.width > avail) {
        box.break_before = true;
        box.x = 0;
    }
    return box;
}

    // change this to use the existing block instead of a new one
public static InlineBox generateReplacedInlineBox(Context c, Node node, int avail, InlineBox prev, String text, InlineBox prev_align, Font font) {
    //u.p("generating replaced Inline Box");
    
    // get the layout for the replaced element
    Layout layout = LayoutFactory.getLayout(node);
    BlockBox block = (BlockBox)layout.layout(c,(Element)node);
    //u.p("got a block box from the sub layout: " + block);
    Rectangle bounds = new Rectangle(block.x,block.y,block.width,block.height);
    //u.p("bounds = " + bounds);
    /* joshy: change this to just modify the existing block instead of creating
    a  new one*/
    
    // create new inline
    InlineBox box = newBox(c,node,0,0,prev,text, bounds, prev_align, font);
    //joshy: activate this: box.block = block
    //u.p("created a new inline box");
    box.replaced = true;
    box.sub_block = block;
    block.setParent(box);
    
    // set up the extents
    box.width = bounds.width;
    box.height = bounds.height;
    box.break_after = false;
    
    // if it won't fit on this line, then put it on the next one
    if(box.width > avail) {
        box.break_before = true;
        box.x = 0;
    }
    
    // return
    //u.p("last replaced = " + box);
    return box;
}

private static InlineBox newBox(Context c, Node node,int start, int end, InlineBox prev, String text, InlineBox prev_align, Font font) {
    return newBox(c,node,start,end,prev,text,null, prev_align, font);
}

// this function by itself takes up fully 29% of the complete program's
// rendering time.
private static InlineBox newBox(Context c, Node node,int start, int end, InlineBox prev, String text, Rectangle bounds, InlineBox prev_align, Font font) {
    //u.p("newBox node = " + node.getNodeName() + " start = " + start + " end = " + end +
    //" prev = " + prev + " text = " + text + " bounds = " + bounds + " prev_align = " + prev_align);
    //u.p("Making box for: "  + node);
    //u.p("prev = " + prev);
     if(prev_align != prev) {
         //u.p("prev = " + prev);
         //u.p("prev align inline = " + prev_align);
     }
    InlineBox box = new InlineBox();
    box.node = node;
    box.start_index = start;
    box.end_index = end;
    /*
    if(prev!= null && !prev.break_after) {
        box.x = prev.x + prev.width;
    } else {
        box.x = 0;
    }
    */

    // use the prev_align to calculate the x
    if(prev_align!= null && !prev_align.break_after) {
        //u.p("moving over w/ prev = " + prev);
        //u.p("moving over w/ prev align = " + prev_align);
        box.x = prev_align.x + prev_align.width;
    } else {
        //u.p("setting x to 0");
        box.x = 0;
    }

    box.y = 0; // it's relative to the line
    try {
        if(!InlineLayout.isReplaced(node)) {
            if(!InlineLayout.isFloatedBlock(node,c)) {
                box.width = FontUtil.len(c,node,text.substring(start,end),font);
            } else {
                box.width = bounds.width;
            }
        } else {
            box.width = bounds.width;
        }
    } catch (StringIndexOutOfBoundsException ex) {
        u.p("ex");
        u.p("start = " + start);
        u.p("end = " + end);
        u.p("text = " + node.getNodeValue());
        throw ex;
    }
    //u.p("box.x = " + box.x);
    if(InlineLayout.isReplaced(node)) {
        box.height = bounds.height;
    } else if(InlineLayout.isFloatedBlock(node,c)) {
        box.height = bounds.height;
    } else {
        box.height = FontUtil.lineHeight(c,node);
    }
    //u.p("box.x = " + box.x);
    //box.baseline = box.height;
    box.break_after = true;

    box.text = text;
    if(!InlineLayout.isReplaced(node)) {
        if(!InlineLayout.isFloatedBlock(node,c)) {
            FontUtil.setupTextDecoration(c,node,box);
            if(box.text == null) {
                return box;
            }
        }
    }
    //u.p("box.x = " + box.x);

    // do vertical alignment
    //u.p("setting up vertical align on: " + node);
    FontUtil.setupVerticalAlign(c,node,box);
    box.setFont(font);//FontUtil.getFont(c,node));
    if(node.getNodeType()== node.TEXT_NODE) {
        box.color = c.css.getColor((Element)node.getParentNode(),true);
    } else {
        box.color = c.css.getColor((Element)node,true);
    }
    InlineLayout.setupRelative(c,box);

    //u.p("box.x = " + box.x);
    //u.p("returning box: " + box);
    //u.p("colo r= " + box.color);
    return box;
}

}