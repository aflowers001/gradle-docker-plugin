/*
 * Copyright 2016 the original author or authors.
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
package com.bmuschko.gradle.docker

import org.gradle.testkit.runner.BuildResult
import spock.lang.Requires

@Requires({ TestPrecondition.DOCKER_SERVER_INFO_URL_REACHABLE })
class DockerWaitForLogMesageInContainerFunctionalTest extends AbstractFunctionalTest {
  def setup() {
    buildFile << """
            import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
            import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
            import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
            import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

            task pullImage(type: DockerPullImage) {
                repository = 'busybox'
                tag = 'latest'
            }

            task createContainer(type: DockerCreateContainer) {
                dependsOn pullImage
                targetImageId { pullImage.repository+":" + pullImage.tag }
                cmd = ['/bin/sh','-c',' for i in 1 2 3 4 5 6 7 8 9 10; do echo "Welcome \$i times"; sleep 1; done']
            }

           task removeContainer(type: DockerRemoveContainer) {
                removeVolumes = true
                force = true
                targetContainerId { startContainer.getContainerId() }
            }

            task workflow {
                dependsOn 'startContainer'

                finalizedBy removeContainer
            }
        """
  }

  def "Can start a container and monitor logs"() {
    buildFile << """
            task startContainer(type: DockerStartContainer) {
                dependsOn createContainer
                targetContainerId { createContainer.getContainerId() }
                
                poller {
                  regex = 'Welcome 5 times'
                }
            }
        """
    expect:
    BuildResult result = build('workflow')
    result.output.contains("Welcome 5 times")
  }

  def "sleep must not be zero"() {
    buildFile << """
            task startContainer(type: DockerStartContainer) {
                dependsOn createContainer
                targetContainerId { createContainer.getContainerId() }
                
                poller {
                  regex = 'snippet'
                  sleep = 0
                }
            }
        """

    expect:
    BuildResult result = buildAndFail('workflow')
  }

  def "sleep must not be less than zero"() {
    buildFile << """
            task startContainer(type: DockerStartContainer) {
                dependsOn createContainer
                targetContainerId { createContainer.getContainerId() }
                
                poller {
                  regex = 'snippet'
                  sleep = -50
                }
            }
        """

    expect:
    BuildResult result = buildAndFail('workflow')
  }

  def "timeout must not be zero"() {
    buildFile << """
            task startContainer(type: DockerStartContainer) {
                dependsOn createContainer
                targetContainerId { createContainer.getContainerId() }
                
                poller {
                  regex = 'snippet'
                  timeout = 0
                }
            }
        """

    expect:
    BuildResult result = buildAndFail('workflow')
  }

  def "timeout must not be less than zero"() {
    buildFile << """
            task startContainer(type: DockerStartContainer) {
                dependsOn createContainer
                targetContainerId { createContainer.getContainerId() }
                
                poller {
                  regex = 'snippet'
                  timeout = -50
                }
            }
        """

    expect:
    BuildResult result = buildAndFail('workflow')
  }

  def "regex must not be empty"() {
    buildFile << """
            task startContainer(type: DockerStartContainer) {
                dependsOn createContainer
                targetContainerId { createContainer.getContainerId() }
                
                poller {
                  regex = ''
                }
            }
        """

    expect:
    BuildResult result = buildAndFail('workflow')
  }

  def "regex must not be null"() {
    buildFile << """
            task startContainer(type: DockerStartContainer) {
                dependsOn createContainer
                targetContainerId { createContainer.getContainerId() }
                
                poller {
                  regex = null
                }
            }
        """

    expect:
    BuildResult result = buildAndFail('workflow')
  }
}

