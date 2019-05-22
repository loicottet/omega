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
//
//  main.m
//  svmsim
//
//  Created by Johan on 19/12/2018.
//  Copyright © 2018 Johan. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "AppDelegate.h"
#include <dirent.h>

int main(int argc, char * argv[]) {
    @autoreleasepool {
        return UIApplicationMain(argc, argv, nil, NSStringFromClass([AppDelegate class]));
    }
}

int JVM_GetInterfaceVersion() {
    return 5;
}

/*
double StrictMath_asin (double b) {
    fprintf(stderr, "[JVDBG] MATH asked\n");
    return 1.;
}

double StrictMath_acos (double b) {
    fprintf(stderr, "[JVDBG] MATH asked\n");
    return 1.;
}
double StrictMath_atan2 (double b) {
    fprintf(stderr, "[JVDBG] MATH asked\n");
    return 1.;
}

double StrictMath_exp (double b) {
    fprintf(stderr, "[JVDBG] MATH asked\n");
    return 1.;
}

double StrictMath_hypot (double b) {
    fprintf(stderr, "[JVDBG] MATH asked\n");
    return 1.;
}

double StrictMath_pow (double a, double b) {
    fprintf(stderr, "[JVDBG] MATH asked\n");
    return 1.;
}
double StrictMath_cbrt (double b) {
    fprintf(stderr, "[JVDBG] MATH asked\n");
    return 1.;
}
*/

void inflateStart() {
    fprintf(stderr, "[JVDBG] INFLATE asked\n");
}

void inflateInit2_() {
    fprintf(stderr, "[JVDBG] INFLATE asked\n");
}
void inflateEnd() {
    fprintf(stderr, "[JVDBG] INFLATE asked\n");
}

void inflate() {
    fprintf(stderr, "[JVDBG] INFLATE asked\n");
}
void inflateReset() {
    fprintf(stderr, "[JVDBG] INFLATE asked\n");
}


struct dirent* readdir_r$INODE64(DIR *dirp) {
    NSLog(@"%@", [NSThread callStackSymbols]);

    //  [NSThread callStackSymbols];
    fprintf(stderr, "[JVDBG] readdir asked\n");
    return readdir(dirp);
}

struct dirent* readdir$INODE64(DIR *dirp) {
    NSLog(@"%@", [NSThread callStackSymbols]);

    //  [NSThread callStackSymbols];
    fprintf(stderr, "[JVDBG] readdir asked\n");
    return readdir(dirp);
}

DIR* opendir$INODE64(const char* dirname) {
    NSLog(@"%@", [NSThread callStackSymbols]);

    fprintf(stderr, "[JVDBG] opendir asked\n");
    return opendir(dirname);
}