/*
 * Selector.java
 * Copyright (c) 2004, 2005 Torbj�rn Gannholm
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
 *
 */
package org.xhtmlrenderer.css.newmatch;

import org.xhtmlrenderer.css.extend.AttributeResolver;
import org.xhtmlrenderer.css.extend.TreeResolver;
import org.xhtmlrenderer.css.sheet.Ruleset;
import org.xhtmlrenderer.util.XRLog;

import java.util.logging.Level;


/**
 * A Selector is really a chain of CSS selectors that all need to be valid for
 * the selector to match.
 *
 * @author Torbj�rn Gannholm
 */
public class Selector {

    /**
     * Description of the Field
     */
    private Ruleset _parent;
    /**
     * Description of the Field
     */
    private Selector chainedSelector = null;
    /**
     * Description of the Field
     */
    private Selector siblingSelector = null;

    /**
     * Description of the Field
     */
    private int _axis;
    /**
     * Description of the Field
     */
    private String _name;
    /**
     * Description of the Field
     */
    private int _pc = 0;
    /**
     * Description of the Field
     */
    private String _pe;

    //specificity - correct values are gotten from the last Selector in the chain
    /**
     * Description of the Field
     */
    private int _specificityB;
    /**
     * Description of the Field
     */
    private int _specificityC;
    /**
     * Description of the Field
     */
    private int _specificityD;

    /**
     * Description of the Field
     */
    private int _pos;//to distinguish between selectors of same specificity

    /**
     * Description of the Field
     */
    private java.util.List conditions;

    /**
     * Description of the Field
     */
    public final static int DESCENDANT_AXIS = 0;
    /**
     * Description of the Field
     */
    public final static int CHILD_AXIS = 1;
    /**
     * Description of the Field
     */
    public final static int IMMEDIATE_SIBLING_AXIS = 2;

    /**
     * Description of the Field
     */
    public final static int VISITED_PSEUDOCLASS = 2;
    /**
     * Description of the Field
     */
    public final static int HOVER_PSEUDOCLASS = 4;
    /**
     * Description of the Field
     */
    public final static int ACTIVE_PSEUDOCLASS = 8;
    /**
     * Description of the Field
     */
    public final static int FOCUS_PSEUDOCLASS = 16;

    /**
     * Give each a unique ID to be able to create a key to internalize Matcher.Mappers
     */
    private int selectorID;
    private static int selectorCount = 0;

    /**
     * Creates a new instance of Selector. Only called in the context of adding
     * a Selector to a Ruleset or adding a chained Selector to another Selector.
     *
     * @param pos         PARAM
     * @param parent      PARAM
     * @param axis        see values above.
     * @param elementName matches any element if null
     */
    Selector(int pos, Ruleset parent, int axis, String elementName) {
        this();
        _parent = parent;
        _axis = axis;
        _name = elementName;
        _pos = pos;
        _specificityB = 0;
        _specificityC = 0;
        _specificityD = 0;
        if (_name != null) {
            _specificityD++;
        }
    }

    /**
     * Constructor for the Selector object
     *
     * @param pos          PARAM
     * @param specificityB PARAM
     * @param specificityC PARAM
     * @param specificityD PARAM
     * @param parent       PARAM
     * @param axis         PARAM
     * @param elementName  PARAM
     */
    private Selector(int pos, int specificityB, int specificityC, int specificityD, Ruleset parent, int axis, String elementName) {
        this(pos, parent, axis, elementName);
        _specificityB += specificityB;
        _specificityC += specificityC;
        _specificityD += specificityD;
    }

    public Selector() {
        selectorID = selectorCount++;
    }

    /**
     * Check if the given Element matches this selector. Note: the parser should
     * give all class
     *
     * @param e       PARAM
     * @param attRes  PARAM
     * @param treeRes
     * @return Returns
     */
    public boolean matches(Object e, AttributeResolver attRes, TreeResolver treeRes) {
        if (siblingSelector != null) {
            Object sib = siblingSelector.getAppropriateSibling(e, treeRes);
            if (sib == null) {
                return false;
            }
            if (!siblingSelector.matches(sib, attRes, treeRes)) {
                return false;
            }
        }
        if (_name == null || _name.equals(treeRes.getElementName(e))) {
            if (conditions != null) {
                // all conditions need to be true
                for (java.util.Iterator i = conditions.iterator(); i.hasNext();) {
                    Condition c = (Condition) i.next();
                    if (!c.matches(e, attRes, treeRes)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Check if the given Element matches this selector's dynamic properties.
     * Note: the parser should give all class
     *
     * @param e       PARAM
     * @param attRes  PARAM
     * @param treeRes
     * @return Returns
     */
    public boolean matchesDynamic(Object e, AttributeResolver attRes, TreeResolver treeRes) {
        if (siblingSelector != null) {
            Object sib = siblingSelector.getAppropriateSibling(e, treeRes);
            if (sib == null) {
                return false;
            }
            if (!siblingSelector.matchesDynamic(sib, attRes, treeRes)) {
                return false;
            }
        }
        if (isPseudoClass(VISITED_PSEUDOCLASS)) {
            if (attRes == null || !attRes.isVisited(e)) {
                return false;
            }
        }
        if (isPseudoClass(ACTIVE_PSEUDOCLASS)) {
            if (attRes == null || !attRes.isActive(e)) {
                return false;
            }
        }
        if (isPseudoClass(HOVER_PSEUDOCLASS)) {
            if (attRes == null || !attRes.isHover(e)) {
                return false;
            }
        }
        if (isPseudoClass(FOCUS_PSEUDOCLASS)) {
            if (attRes == null || !attRes.isFocus(e)) {
                return false;
            }
        }
        return true;
    }

    /**
     * append a selector to this chain, specifying which axis it should be
     * evaluated on
     *
     * @param axis        PARAM
     * @param elementName PARAM
     * @return Returns
     */
    public Selector appendChainedSelector(int axis, String elementName) {
        checkPseudoElement();
        if (chainedSelector == null) {
            return (chainedSelector = new Selector(_pos, getSpecificityB(), getSpecificityC(), getSpecificityD(), _parent, axis, elementName));
        } else {
            return chainedSelector.appendChainedSelector(axis, elementName);
        }
    }

    /**
     * append a selector to this chain, specifying which axis it should be
     * evaluated on
     *
     * @param axis        PARAM
     * @param elementName PARAM
     * @return Returns
     */
    public Selector appendSiblingSelector(int axis, String elementName) {
        checkPseudoElement();
        if (siblingSelector == null) {
            return (siblingSelector = new Selector(_pos, getSpecificityB(), getSpecificityC(), getSpecificityD(), _parent, axis, elementName));
        } else {
            return siblingSelector.appendSiblingSelector(axis, elementName);
        }
    }

    private void checkPseudoElement() {
        if (_pe != null) {
            addUnsupportedCondition();
            XRLog.match(Level.WARNING, "Trying to append child selectors to pseudoElement " + _pe);
        }
    }

    /**
     * for unsupported or invalid CSS
     */
    public void addUnsupportedCondition() {
        addCondition(Condition.createUnsupportedCondition());
    }

    /**
     * the CSS condition that element has pseudo-class :link
     */
    public void addLinkCondition() {
        _specificityC++;
        addCondition(Condition.createLinkCondition());
    }

    /**
     * the CSS condition that element has pseudo-class :first-child
     */
    public void addFirstChildCondition() {
        _specificityC++;
        addCondition(Condition.createFirstChildCondition());
    }

    /**
     * the CSS condition :lang(Xx)
     *
     * @param lang The feature to be added to the LangCondition attribute
     */
    public void addLangCondition(String lang) {
        _specificityC++;
        addCondition(Condition.createLangCondition(lang));
    }

    /**
     * the CSS condition #ID
     *
     * @param id The feature to be added to the IDCondition attribute
     */
    public void addIDCondition(String id) {
        _specificityB++;
        addCondition(Condition.createIDCondition(id));
    }

    /**
     * the CSS condition .class
     *
     * @param className The feature to be added to the ClassCondition attribute
     */
    public void addClassCondition(String className) {
        _specificityC++;
        addCondition(Condition.createClassCondition(className));
    }

    /**
     * the CSS condition [attribute]
     *
     * @param name The feature to be added to the AttributeExistsCondition
     *             attribute
     */
    public void addAttributeExistsCondition(String name) {
        _specificityC++;
        addCondition(Condition.createAttributeExistsCondition(name));
    }

    /**
     * the CSS condition [attribute=value]
     *
     * @param name  The feature to be added to the AttributeEqualsCondition
     *              attribute
     * @param value The feature to be added to the AttributeEqualsCondition
     *              attribute
     */
    public void addAttributeEqualsCondition(String name, String value) {
        _specificityC++;
        addCondition(Condition.createAttributeEqualsCondition(name, value));
    }

    /**
     * the CSS condition [attribute~=value]
     *
     * @param name  The feature to be added to the
     *              AttributeMatchesListCondition attribute
     * @param value The feature to be added to the
     *              AttributeMatchesListCondition attribute
     */
    public void addAttributeMatchesListCondition(String name, String value) {
        _specificityC++;
        addCondition(Condition.createAttributeMatchesListCondition(name, value));
    }

    /**
     * the CSS condition [attribute|=value]
     *
     * @param name  The feature to be added to the
     *              AttributeMatchesFirstPartCondition attribute
     * @param value The feature to be added to the
     *              AttributeMatchesFirstPartCondition attribute
     */
    public void addAttributeMatchesFirstPartCondition(String name, String value) {
        _specificityC++;
        addCondition(Condition.createAttributeMatchesFirstPartCondition(name, value));
    }

    /**
     * set which pseudoclasses must apply for this selector
     *
     * @param pc the values from AttributeResolver should be used. Once set
     *           they cannot be unset. Note that the pseudo-classes should be set one
     *           at a time, otherwise specificity of declaration becomes wrong.
     */
    public void setPseudoClass(int pc) {
        if (!isPseudoClass(pc)) {
            _specificityC++;
        }
        _pc |= pc;
    }

    /**
     * check if selector queries for dynamic properties
     *
     * @param pseudoElement The new pseudoElement value
     */
    /*
     * public boolean isDynamic() {
     * return (_pc != 0);
     * }
     */
    public void setPseudoElement(String pseudoElement) {
        if (_pe != null) {
            addUnsupportedCondition();
            XRLog.match(Level.WARNING, "Trying to set more than one pseudo-element");
        } else {
            _specificityD++;
            _pe = pseudoElement;
        }
    }

    /**
     * query if a pseudoclass must apply for this selector
     *
     * @param pc the values from AttributeResolver should be used.
     * @return The pseudoClass value
     */
    public boolean isPseudoClass(int pc) {
        return ((_pc & pc) != 0);
    }

    /**
     * Gets the pseudoElement attribute of the Selector object
     *
     * @return The pseudoElement value
     */
    public String getPseudoElement() {
        //only care about the last in the chain
        if (chainedSelector != null) {
            return chainedSelector.getPseudoElement();
        } else {
            return _pe;
        }
    }

    /**
     * get the next selector in the chain, for matching against elements along
     * the appropriate axis
     *
     * @return The chainedSelector value
     */
    public Selector getChainedSelector() {
        return chainedSelector;
    }

    /**
     * get the Ruleset that this Selector is part of
     *
     * @return The ruleset value
     */
    public Ruleset getRuleset() {
        return _parent;
    }

    /**
     * get the axis that this selector should be evaluated on
     *
     * @return The axis value
     */
    public int getAxis() {
        return _axis;
    }

    /**
     * The correct specificity value for this selector and its sibling-axis
     * selectors
     */
    public int getSpecificityB() {
        if (siblingSelector != null) {
            return siblingSelector.getSpecificityB();
        }
        return _specificityB;
    }

    /**
     * The correct specificity value for this selector and its sibling-axis
     * selectors
     */
    public int getSpecificityD() {
        if (siblingSelector != null) {
            return siblingSelector.getSpecificityD();
        }
        return _specificityD;
    }

    /**
     * The correct specificity value for this selector and its sibling-axis
     * selectors
     */
    public int getSpecificityC() {
        if (siblingSelector != null) {
            return siblingSelector.getSpecificityC();
        }
        return _specificityC;
    }

    /**
     * returns "a number in a large base" with specificity and specification
     * order of selector
     *
     * @return The order value
     */
    String getOrder() {
        if (chainedSelector != null) {
            return chainedSelector.getOrder();
        }//only "deepest" value is correct
        String b = "000" + getSpecificityB();
        String c = "000" + getSpecificityC();
        String d = "000" + getSpecificityD();
        String p = "00000" + _pos;
        return "0" + b.substring(b.length() - 3) + c.substring(c.length() - 3) + d.substring(d.length() - 3) + p.substring(p.length() - 5);
    }

    /**
     * Gets the appropriateSibling attribute of the Selector object
     *
     * @param e       PARAM
     * @param treeRes
     * @return The appropriateSibling value
     */
    Object getAppropriateSibling(Object e, TreeResolver treeRes) {
        Object sibling = null;
        switch (_axis) {
            case IMMEDIATE_SIBLING_AXIS:
                sibling = treeRes.getPreviousSiblingElement(e);
                break;
            default:
                XRLog.exception("Bad sibling axis");
        }
        return sibling;
    }

    /**
     * Adds a feature to the Condition attribute of the Selector object
     *
     * @param c The feature to be added to the Condition attribute
     */
    private void addCondition(Condition c) {
        if (conditions == null) {
            conditions = new java.util.ArrayList();
        }
        if (_pe != null) {
            conditions.add(Condition.createUnsupportedCondition());
            XRLog.match(Level.WARNING, "Trying to append conditions to pseudoElement " + _pe);
        }
        conditions.add(c);
    }

    /**
     * Gets the elementStylingOrder attribute of the Selector class
     *
     * @return The elementStylingOrder value
     */
    static String getElementStylingOrder() {
        return "1" + "000" + "000" + "000" + "00000";
    }

    public int getSelectorID() {
        return selectorID;
    }
    
    public void setName(String name) {
        _name = name;
    }
    
    public void setPos(int pos) {
        _pos = pos;
        if (siblingSelector != null) {
            siblingSelector.setPos(pos);
        }
        if (chainedSelector != null) {
            chainedSelector.setPos(pos);
        }
    }
    
    public void setParent(Ruleset ruleset) {
        _parent = ruleset;
    }
    
    public void setAxis(int axis) {
        _axis = axis;
    }
    
    public void setSpecificityB(int b) {
        _specificityB = b;
    }
    
    public void setSpecificityC(int c) {
        _specificityC = c;
    }
    
    public void setSpecificityD(int d) {
        _specificityD = d;
    }
    
    public void setChainedSelector(Selector selector) {
        chainedSelector = selector;
    }
    
    public void setSiblingSelector(Selector selector) {
        siblingSelector = selector;
    }
}

