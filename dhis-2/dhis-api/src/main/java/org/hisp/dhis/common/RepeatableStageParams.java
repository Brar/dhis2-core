/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.common;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RepeatableStageParams
{
    private int startIndex;

    private int count = 1;

    // related to execution date
    private Date startDate;

    // related to execution date
    private Date endDate;

    @Override
    public String toString()
    {
        return "startIndex:" + startIndex + " count:"
            + (count == Integer.MAX_VALUE ? "all" : count)
            + " startDate:" + startDate + " endDate: " + endDate;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( !(o instanceof RepeatableStageParams) )
        {
            return false;
        }

        RepeatableStageParams other = (RepeatableStageParams) o;

        return other.startIndex == startIndex && other.count == count
            && other.startDate != null ? other.startDate.equals( startDate )
                : startDate == null
                    && other.endDate != null ? other.endDate.equals( endDate ) : endDate == null;
    }

    @Override
    public int hashCode()
    {
        return (startIndex + count)
            * (startDate == null ? "null".hashCode() : startDate.hashCode())
            * (endDate == null ? "null".hashCode() : endDate.hashCode());
    }

    public boolean isNumberValueType()
    {
        return count == 1;
    }
}
