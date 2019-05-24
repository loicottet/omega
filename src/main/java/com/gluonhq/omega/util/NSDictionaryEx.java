/*
 * Copyright (c) 2019, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.omega.util;

import com.dd.plist.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class NSDictionaryEx {

    NSDictionary dict; //TODO make private once API is stable

    public NSDictionaryEx() {
        this.dict = new NSDictionary();
    }

    public NSDictionaryEx( NSDictionary dict ) {
        this.dict = Objects.requireNonNull(dict);
    }

    public NSDictionaryEx( Path path ) throws ParserConfigurationException, ParseException, SAXException, PropertyListFormatException, IOException {
        this((NSDictionary) PropertyListParser.parse(path.toFile()));
    }

    public NSDictionaryEx( File file ) throws ParserConfigurationException, ParseException, SAXException, PropertyListFormatException, IOException {
        this((NSDictionary) PropertyListParser.parse(file));
    }

    public NSDictionaryEx( String filePath ) throws PropertyListFormatException, ParserConfigurationException, SAXException, ParseException, IOException {
        this((NSDictionary) PropertyListParser.parse(filePath));
    }

    public NSDictionaryEx( byte[] bytes ) throws ParserConfigurationException, ParseException, SAXException, PropertyListFormatException, IOException {
        this((NSDictionary) PropertyListParser.parse(bytes));
    }

    public NSDictionaryEx( InputStream inputStream ) throws ParserConfigurationException, ParseException, SAXException, PropertyListFormatException, IOException {
        this((NSDictionary) PropertyListParser.parse(inputStream));
    }

//    public NSDictionaryEx(Pair<String,Object>... kvPairs) {
//        this();
//        for ( Pair<String,Object> pair: kvPairs ) {
//            dict.put(pair.getKey(), pair.getValue());
//        }
//
//    }

    public void saveAsXML( Path destination) throws IOException {
        PropertyListParser.saveAsXML(dict, destination.toFile());
    }

    public void saveAsBinary( Path destination) throws IOException {
        PropertyListParser.saveAsBinary(dict, destination.toFile());
    }

    public void put( String key, Object value) {
        dict.put(key, value);
    }

    public void put( String key, NSObject value) {
        dict.put(key, value);
    }

    public String[] getAllKeys() {
        return dict.allKeys();
    }

    public Set<String> getKeySet() {
        return dict.keySet();
    }

    public Set<Map.Entry<String, NSObject>> getEntrySet() {
        return dict.entrySet();
    }

    public NSObject get( String key ) {
        return dict.objectForKey(key);
    }

    public void remove( String key ) {
        dict.remove(key);
    }


    public boolean getBoolean( String key ) {
        return ((NSNumber) dict.objectForKey(key)).boolValue();
    }

    public String getString( String key ) {
        NSString nsString = (NSString) dict.objectForKey(key);
        if (nsString != null) {
            return nsString.toString();
        }
        return "";
    }

    public LocalDate getDate(String key ) {
        Date date =  ((NSDate) dict.objectForKey(key)).getDate();
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }


    public NSObject[] getArray( String key ) {
        return ((NSArray) dict.objectForKey(key)).getArray();
    }

    public NSDictionaryEx getDictionary( String key ) {
        return new NSDictionaryEx(((NSDictionary) dict.objectForKey(key)));
    }




}
