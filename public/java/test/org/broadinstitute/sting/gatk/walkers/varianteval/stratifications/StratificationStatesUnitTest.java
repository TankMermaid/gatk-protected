/*
 * Copyright (c) 2012, The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

// our package
package org.broadinstitute.sting.gatk.walkers.varianteval.stratifications;


// the imports for unit testing.


import org.broadinstitute.sting.BaseTest;
import org.broadinstitute.sting.utils.Utils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.util.*;


public class StratificationStatesUnitTest extends BaseTest {
    @BeforeClass
    public void init() throws FileNotFoundException {
    }

    // --------------------------------------------------------------------------------
    //
    // Basic tests Provider
    //
    // --------------------------------------------------------------------------------

    private class StratificationStatesTestProvider extends TestDataProvider {
        final List<List<Object>> allStates = new ArrayList<List<Object>>();
        final List<ListAsSetOfStates> asSetOfStates = new ArrayList<ListAsSetOfStates>();
        final int nStates;
        
        public StratificationStatesTestProvider(final List<Integer> ... allStates) {
            super(StratificationStatesTestProvider.class);
            
            for ( List<Integer> states : allStates ) {
                this.allStates.add(new ArrayList<Object>(states));
            }

            for ( List<Object> states : this.allStates ) { 
                asSetOfStates.add(new ListAsSetOfStates(states));
            }
            this.nStates = Utils.nCombinations(allStates);

            setName(getName());
        }

        private String getName() {
            StringBuilder b = new StringBuilder();
            int c = 1;
            for ( List<Object> state : allStates )
                b.append(String.format("%d = [%s] ", c++, Utils.join(",", state)));
            return b.toString();
        }
        
        public List<ListAsSetOfStates> getStateSpaceList() {
            return asSetOfStates;
        }
        
        public Queue<List<Object>> getAllCombinations() {
            return getAllCombinations(new LinkedList<List<Object>>(allStates));
        }

        private Queue<List<Object>> getAllCombinations(Queue<List<Object>> states) {
            if ( states.isEmpty() ) 
                return new LinkedList<List<Object>>();
            else {
                List<Object> head = states.poll();
                Queue<List<Object>> substates = getAllCombinations(states);
                Queue<List<Object>> newStates = new LinkedList<List<Object>>();
                for ( final Object e : head) {
                    if ( substates.isEmpty() ) {
                        newStates.add(new LinkedList<Object>(Collections.singleton(e)));
                    } else {
                        for ( final List<Object> state : substates ) {
                            List<Object> newState = new LinkedList<Object>();
                            newState.add(e);
                            newState.addAll(state);
                            newStates.add(newState);
                        }
                    }
                }
                return newStates;
            }
        }
    }

    private class ListAsSetOfStates implements SetOfStates {
        final List<Object> integers;

        private ListAsSetOfStates(final List<Object> integers) {
            this.integers = integers;
        }
        
        @Override
        public List<Object> getAllStates() {
            return integers;
        }
    }

    @DataProvider(name = "StratificationStatesTestProvider")
    public Object[][] makeStratificationStatesTestProvider() {
        new StratificationStatesTestProvider(Arrays.asList(0));
        new StratificationStatesTestProvider(Arrays.asList(0, 1));
        new StratificationStatesTestProvider(Arrays.asList(0, 1), Arrays.asList(2, 3));
        new StratificationStatesTestProvider(Arrays.asList(0, 1), Arrays.asList(2, 3), Arrays.asList(4, 5));
        new StratificationStatesTestProvider(Arrays.asList(0, 1), Arrays.asList(2, 3, 4), Arrays.asList(5, 6));
        new StratificationStatesTestProvider(Arrays.asList(0, 1), Arrays.asList(2, 3, 4, 5), Arrays.asList(6));
        new StratificationStatesTestProvider(Arrays.asList(0, 1), Arrays.asList(2, 3, 4, 5), Arrays.asList(6, 7));
        new StratificationStatesTestProvider(Arrays.asList(0, 1), Arrays.asList(2, 3), Arrays.asList(4, 5), Arrays.asList(6, 7));
        return StratificationStatesTestProvider.getTests(StratificationStatesTestProvider.class);
    }

    @Test(dataProvider = "StratificationStatesTestProvider")
    public void testLeafCount(StratificationStatesTestProvider cfg) {
        final StratificationStates<ListAsSetOfStates> stratificationStates = new StratificationStates<ListAsSetOfStates>(cfg.getStateSpaceList());

        Assert.assertEquals(stratificationStates.getNStates(), cfg.nStates);
        
        int nLeafs = 0;
        for ( final StratNode node : stratificationStates.getRoot() ) {
            if ( node.isLeaf() )
                nLeafs++;
        }
        Assert.assertEquals(nLeafs, cfg.nStates, "Unexpected number of leaves");
    }

    @Test(dataProvider = "StratificationStatesTestProvider")
    public void testKeys(StratificationStatesTestProvider cfg) {
        final StratificationStates<ListAsSetOfStates> stratificationStates = new StratificationStates<ListAsSetOfStates>(cfg.getStateSpaceList());
        final Set<Integer> seenKeys = new HashSet<Integer>(cfg.nStates);
        for ( final StratNode node : stratificationStates.getRoot() ) {
            if ( node.isLeaf() ) {
                Assert.assertFalse(seenKeys.contains(node.getKey()), "Already seen the key");
                seenKeys.add(node.getKey());
            }
        }
    }

    @Test(dataProvider = "StratificationStatesTestProvider")
    public void testFindSingleKeys(StratificationStatesTestProvider cfg) {
        final StratificationStates<ListAsSetOfStates> stratificationStates = new StratificationStates<ListAsSetOfStates>(cfg.getStateSpaceList());
        final Set<Integer> seenKeys = new HashSet<Integer>(cfg.nStates);
        for ( List<Object> state : cfg.getAllCombinations() ) {
            final int key = stratificationStates.getKey(state);
            Assert.assertFalse(seenKeys.contains(key), "Already saw state mapping to this key");
            seenKeys.add(key);
        }
    }

    @Test(dataProvider = "StratificationStatesTestProvider")
    public void testFindMultipleKeys(StratificationStatesTestProvider cfg) {
        final StratificationStates<ListAsSetOfStates> stratificationStates = new StratificationStates<ListAsSetOfStates>(cfg.getStateSpaceList());
        final List<List<Object>> states = new ArrayList<List<Object>>(cfg.allStates);
        final Set<Integer> keys = stratificationStates.getKeys(states);
        Assert.assertEquals(keys.size(), cfg.nStates, "Find all states didn't find all of the expected unique keys");

        final Queue<List<Object>> combinations = cfg.getAllCombinations();
        while ( ! combinations.isEmpty() ) {
            List<Object> first = combinations.poll();
            List<Object> second = combinations.peek();
            if ( second != null ) {
                List<List<Object>> combined = StratificationStates.combineStates(first, second);
                int nExpectedKeys = Utils.nCombinations(combined);

                final int key1 = stratificationStates.getKey(first);
                final int key2 = stratificationStates.getKey(second);
                final Set<Integer> keysCombined = stratificationStates.getKeys(combined);
            
                Assert.assertTrue(keysCombined.contains(key1), "couldn't find key in data set");
                Assert.assertTrue(keysCombined.contains(key2), "couldn't find key in data set");
                
                Assert.assertEquals(keysCombined.size(), nExpectedKeys);
            }
        }
    }
}