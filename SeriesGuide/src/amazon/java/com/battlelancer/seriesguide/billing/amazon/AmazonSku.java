/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.billing.amazon;

/**
 * Contains all Amazon in-app purchase products definitions.
 */
public enum AmazonSku {

    /**
     * The unlock all subscription parent SKU.
     *
     * <p>This SKU is used in receipts (the actual purchased period SKU is NOT used, instead the
     * purchase and cancel date of the receipt are populated).
     */
    SERIESGUIDE_SUB_PARENT("seriesguide-sub"),
    /**
     * The unlock all subscription yearly period SKU with a trial month.
     *
     * <p>{@code seriesguide-sub} is the parent SKU, the unlock all subscription. It may have
     * multiple children with different periods. Currently we only have one yearly period.
     */
    SERIESGUIDE_SUB_YEARLY("seriesguide-sub-year"),
    /**
     * The one-time purchase. Unlocks access to everything.
     */
    SERIESGUIDE_PASS("x-pass-2014-10");

    private final String sku;

    public String getSku() {
        return this.sku;
    }

    AmazonSku(final String sku) {
        this.sku = sku;
    }

    public static AmazonSku fromSku(final String sku) {
        if (SERIESGUIDE_SUB_PARENT.getSku().equals(sku)) {
            return SERIESGUIDE_SUB_PARENT;
        }
        if (SERIESGUIDE_SUB_YEARLY.getSku().equals(sku)) {
            return SERIESGUIDE_SUB_YEARLY;
        }
        if (SERIESGUIDE_PASS.getSku().equals(sku)) {
            return SERIESGUIDE_PASS;
        }
        return null;
    }

}
