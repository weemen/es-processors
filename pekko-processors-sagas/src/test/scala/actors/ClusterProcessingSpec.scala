package actors

import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.Eventually
import processors.{BaseProcessor, TestEventA, TestEventB}
import scala.concurrent.duration.*

class ClusterProcessingSpec extends AnyWordSpec with Matchers with Eventually {

  def config(port: Int) = ConfigFactory
    .parseString(s"""
      |pekko.remote.artery.canonical.port = $port
      |""".stripMargin)
    .withFallback(ConfigFactory.load("cluster-test.conf"))

  "Processing system in a cluster" should {
    "reach ProcessingActor on a different node" in {
      val system1 = ActorSystem(Behaviors.empty, "ClusterSystem", config(2551))
      val system2 = ActorSystem(Behaviors.empty, "ClusterSystem", config(0))

      try {
        val cluster1 = Cluster(system1)
        val cluster2 = Cluster(system2)

        eventually(timeout(10.seconds)) {
          cluster1.state.members.size shouldBe 2
          cluster2.state.members.size shouldBe 2
        }

        val processor = new MockProcessingProcessor(List(TestEventA, TestEventB))
        // Spawn ProcessingActor on node 1
        system1.receptionist ! org.apache.pekko.actor.typed.receptionist.Receptionist.Register(
          org.apache.pekko.actor.typed.receptionist.ServiceKey[Any]("worker-remote-actor"),
          system1.systemActorOf(ProcessingActor("remote-actor", processor), "remote-actor")
        )

        // Spawn ProcessorManagerActor on node 2
        val manager = system2.systemActorOf(ProcessorManagerActor("manager-2"), "manager-2")
        manager ! RegisterProcessor(processor)

        // Wait a bit for receptionist to propagate
        Thread.sleep(2000)

        // Send event via manager on node 2
        manager ! ProcessEvent(TestEventA("cluster-test"), "remote-actor")
        manager ! ProcessEvent(TestEventB(99), "remote-actor")

        eventually(timeout(5.seconds)) {
          processor.getEventByType[TestEventA] shouldBe Some(TestEventA("cluster-test"))
          processor.getEventByType[TestEventB] shouldBe Some(TestEventB(99))
        }

      } finally {
        system1.terminate()
        system2.terminate()
      }
    }
  }
}
