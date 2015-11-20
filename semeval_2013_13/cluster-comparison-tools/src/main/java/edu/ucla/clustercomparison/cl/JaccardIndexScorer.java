/*
 * Copyright 2013 David Jurgens
 *
 * This file is part of the Cluster-Comparison package and is covered under the
 * terms and conditions therein.
 *
 * The Cluster-Comparison package is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation and distributed hereunder to
 * you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.clustercomparison.cl;

import edu.ucla.clustercomparison.BaseScorer;
import edu.ucla.clustercomparison.Evaluation;
import edu.ucla.clustercomparison.JaccardIndex;


/**
 * The command-line program for comparing two sense labelings using {@link
 * JaccardIndex}.
 */
public class JaccardIndexScorer extends CliRunner {

    @Override protected Evaluation getEvaluation() {
        return new JaccardIndex();
    }

    @Override protected String getEvalName() {
        return "Jaccard Index";
    }

    public static void main(String[] args) throws Exception {   
        new JaccardIndexScorer().run(args);
    } 
}
