/*
 * Ruleset.java
 * Copyright (c) 2004, 2005 Patrick Wright, Torbj�rn Gannholm
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
package org.xhtmlrenderer.css.sheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.xhtmlrenderer.context.CSSPageRuleAdapter;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.newmatch.Selector;
import org.xhtmlrenderer.util.XRLog;

import com.steadystate.css.dom.CSSStyleRuleImpl;


/**
 * @author Torbj�rn Gannholm
 * @author Patrick Wright
 */
public class Ruleset {
    private int _origin;
    private java.util.List _props;

    /**
     * Our list of SAC Selectors, pulled from the CSSStyleRule used to
     * initialize this Ruleset
     */
    private org.w3c.css.sac.SelectorList sacSelectorList;

    private String _selectorText;
    
    private List _fsSelectors = new ArrayList();

    public Ruleset(org.w3c.dom.css.CSSStyleRule rule, int orig) {
        this(orig);
        this._selectorText = rule.getSelectorText();
        pullPropertiesFromDOMRule(rule);
        pullSelectorsFromDOMRule(rule);
    }

    /**
     * Instantiates a Ruleset for a specific {@link org.w3c.css.sac.SelectorList}, List of
     * {@link PropertyDeclaration} and origin. Can be used when you have these
     * outside of a {@link org.w3c.dom.css.CSSStyleRule}.
     *
     * @param selectorList         PARAM
     * @param propertyDeclarations PARAM
     * @param orig                 PARAM
     */
    public Ruleset(org.w3c.css.sac.SelectorList selectorList, List propertyDeclarations, int orig) {
        this(orig);
        this.sacSelectorList = selectorList;
        this._props.addAll(propertyDeclarations);
    }

    public Ruleset(int orig) {
        _origin = orig;
        _props = new LinkedList();
        _fsSelectors = new LinkedList();
    }

    /**
     * Returns an Iterator of PropertyDeclarations pulled from this
     * CSSStyleRule.
     *
     * @return The propertyDeclarations value
     */
    public List getPropertyDeclarations() {
        return Collections.unmodifiableList(_props);
    }

    /**
     * Returns the SAC SelectorList associated with this CSSStyleRule.
     *
     * @return The selectorList value
     */
    public org.w3c.css.sac.SelectorList getSelectorList() {
        return sacSelectorList;
    }

    /**
     * Extracts the CSS SAC SelectorList from a CSSStyleRule.
     *
     * @param sacRule PARAM
     */
    private void pullSelectorsFromDOMRule(org.w3c.dom.css.CSSStyleRule sacRule) {
        if (sacRule instanceof CSSPageRuleAdapter) {
            return;
        }
        // HACK, but right now we already depend on Steady State classes
        sacSelectorList = ((CSSStyleRuleImpl) sacRule).getSelectorList();

        /*
        // note, we parse the selector for this instance, not the one from the CSS Style, which
        // might still be multi-part; selector for TODO is always single (no commas)
        
        sacSelectorList =
                CSOM_PARSER.parseSelectors(new org.w3c.css.sac.InputSource(new java.io.StringReader(sacRule.getSelectorText())));
        */
    }

    /**
     * Given a CSSStyleRule, pulls all properties into instances of
     * PropertyDeclaration which are stored in our _props List.
     *
     * @param sacRule PARAM
     */
    private void pullPropertiesFromDOMRule(org.w3c.dom.css.CSSStyleRule sacRule) {
        org.w3c.dom.css.CSSStyleDeclaration decl = sacRule.getStyle();

        // a style declaration is a block of property assignments
        // so looping items in the declaration means looping properties
        //
        // here we create a PropertyDeclaration for each property, expanding
        // shorthand properties along the way.
        for (int i = 0; i < decl.getLength(); i++) {
            String propName = decl.item(i);
            CSSName cssName = CSSName.getByPropertyName(propName);

            if ( cssName == null ) {
                // TODO: we don't pass unknown properties through. Right now new properties need to be declared in CSSName, and there is no dynamic way to do this (outside of a compile)
                XRLog.cascade("Unknown property in stylesheet: " + propName + ", skipping it.");
            } else {
                try {
                Iterator iter = PropertyDeclaration.newFactory(cssName).buildDeclarations(decl, cssName, _origin);

                while (iter.hasNext()) {
                    // the cast is just for doc purposes
                    _props.add((PropertyDeclaration) iter.next());
                }
                } catch (Exception ex) {
                    XRLog.cascade(
                            Level.WARNING,
                            "Property " + cssName + " could not be parsed while creating PropertyDeclarations. " +
                            "Assigned value might be: " + decl.getPropertyValue(cssName.toString()) + ". " +
                            "Exception was " + ex + ". Property is being IGNORED and skipped."
                    );
                }
            }
        }
    }

    public String getSelectorText() {
        return _selectorText;
    }
    
    public void setSelectorText(String selectorText) {
        _selectorText = selectorText;
    }
    
    public void addProperty(PropertyDeclaration decl) {
        _props.add(decl);
    }
    
    public void addAllProperties(List props) {
        _props.addAll(props);
    }
    
    public void addFSSelector(Selector selector) {
        _fsSelectors.add(selector);
    }
    
    public List getFSSelectors() {
        return _fsSelectors;
    }
    
    public int getOrigin() {
        return _origin;
    }

}// end class

/*
 * $Id$
 *
 * $Log$
 * Revision 1.15  2007/02/19 14:53:38  peterbrant
 * Integrate new CSS parser
 *
 * Revision 1.14  2006/07/26 18:05:05  pdoubleya
 * Clean exception throw.
 *
 * Revision 1.13  2006/05/08 21:36:03  pdoubleya
 * Log and skip properties we can't parse into declarations...
 *
 * Revision 1.12  2005/12/30 01:32:41  peterbrant
 * First merge of parts of pagination work
 *
 * Revision 1.11  2005/10/20 20:48:05  pdoubleya
 * Updates for refactoring to style classes. CalculatedStyle now has lookup methods to cover all general cases, so propertyByName() is private, which means the backing classes for styling were able to be replaced.
 *
 * Revision 1.10  2005/10/15 23:39:15  tobega
 * patch from Peter Brant
 *
 * Revision 1.9  2005/07/14 17:43:39  joshy
 * fixes for parser access exceptions when running in a sandbox (webstart basically)
 *
 * Revision 1.8  2005/06/16 07:24:46  tobega
 * Fixed background image bug.
 * Caching images in browser.
 * Enhanced LinkListener.
 * Some house-cleaning, playing with Idea's code inspection utility.
 *
 * Revision 1.7  2005/01/29 20:19:21  pdoubleya
 * Clean/reformat code. Removed commented blocks, checked copyright.
 *
 * Revision 1.6  2005/01/29 12:08:23  pdoubleya
 * Added constructor for SelectorList/PD List, for possible use of our own SAC DocumentHandler in the future.
 *
 * Revision 1.5  2005/01/24 19:01:08  pdoubleya
 * Mass checkin. Changed to use references to CSSName, which now has a Singleton instance for each property, everywhere property names were being used before. Removed commented code. Cascaded and Calculated style now store properties in arrays rather than maps, for optimization.
 *
 * Revision 1.4  2005/01/24 14:36:30  pdoubleya
 * Mass commit, includes: updated for changes to property declaration instantiation, and new use of DerivedValue. Removed any references to older XR... classes (e.g. XRProperty). Cleaned imports.
 *
 * Revision 1.3  2004/11/15 12:42:23  pdoubleya
 * Across this checkin (all may not apply to this particular file)
 * Changed default/package-access members to private.
 * Changed to use XRRuntimeException where appropriate.
 * Began move from System.err.println to std logging.
 * Standard code reformat.
 * Removed some unnecessary SAC member variables that were only used in initialization.
 * CVS log section.
 *
 *
 */

