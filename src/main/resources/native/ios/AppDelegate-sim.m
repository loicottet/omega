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
#import "AppDelegate.h"

int startGVM();

extern void *JavaMainWrapper__run__5087f5482cc9a6abc971913ece43acb471d2631b();

@interface AppDelegate ()

@end


@implementation AppDelegate

-(void)startVM:(id)selector {
    fprintf(stderr, "Starting vm...\n");
    startGVM();
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    fprintf(stderr, "UIApplication launched!\n");
    [self performSelectorInBackground:@selector(startVM:) withObject:NULL];
    fprintf(stderr, "UIApplication started GVM in a separate thread\n");
    return YES;
}


- (void)applicationWillResignActive:(UIApplication *)application {
    fprintf(stderr, "[UIAPP] applicationWillResignActive\n");
}


- (void)applicationDidEnterBackground:(UIApplication *)application {
    fprintf(stderr, "[UIAPP] applicationDidEnterBackground\n");
}


- (void)applicationWillEnterForeground:(UIApplication *)application {
    fprintf(stderr, "[UIAPP] applicationWillEnterForeground\n");
}


- (void)applicationDidBecomeActive:(UIApplication *)application {
    fprintf(stderr, "[UIAPP] applicationDidBecomeActive\n");
}


- (void)applicationWillTerminate:(UIApplication *)application {
    fprintf(stderr, "[UIAPP] applicationWillTerminate\n");
}


@end


int startGVM() {
    int ret;
    fprintf(stderr, "Starting GVM\n");
/*
    fprintf(stderr, "Starting GVM, create isolatehread\n");
    graal_create_isolate_params_t isolate_params;
    graal_isolate_t* isolate;
    graal_isolatethread_t* isolatethread;
    ret = graal_create_isolate(&isolate_params, &isolate, &isolatethread);
    if (ret != 0) {
        fprintf(stderr, "Whoops, can't create isolate\n");
    }
*/
    (*JavaMainWrapper__run__5087f5482cc9a6abc971913ece43acb471d2631b)(1);

    fprintf(stderr, "Finished running GVM, done with isolatehread\n");
    return 0;
}

