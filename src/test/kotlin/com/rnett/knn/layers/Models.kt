package com.rnett.knn.layers

import com.rnett.knn.components.Activations
import com.rnett.knn.components.Losses
import com.rnett.knn.components.Optimizers
import com.rnett.knn.layers.convolutional.Convolution2DLayer
import com.rnett.knn.layers.convolutional.Subsampling2DLayer
import com.rnett.knn.layers.feedforeward.DenseLayer
import com.rnett.knn.layers.feedforeward.loss.LossLayer
import com.rnett.knn.layers.feedforeward.output.OutputLayer
import com.rnett.knn.layers.util.ActivationLayer
import com.rnett.knn.models.graph
import com.rnett.knn.p2
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator
import org.junit.Test
import org.nd4j.evaluation.classification.Evaluation
import org.nd4j.linalg.activations.impl.ActivationLReLU
import org.nd4j.linalg.activations.impl.ActivationSoftmax
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.impl.LossNegativeLogLikelihood
import org.nd4j.nativeblas.NativeOpsHolder
import org.nd4j.nativeblas.Nd4jBlas
import kotlin.system.measureTimeMillis


class ConvTests {

    @Test
    fun `test mnist capsnet`() {

        val nd4jBlas = Nd4j.factory().blas() as Nd4jBlas
        nd4jBlas.maxThreads = 6

        val instance = NativeOpsHolder.getInstance()
        val deviceNativeOps = instance.deviceNativeOps
        deviceNativeOps.setOmpNumThreads(6)

        val model = graph {
            val image = input("image", 1, 28, 28)

            optimizer = Optimizers.Adam

            image.reshape(1, 28, 28) // the input is flat, reshape it

            println(image.shape)

            image {
                +Convolution2DLayer(256, 9.p2) { activation = ActivationLReLU() }
                println("Conv: ${image.shape}")
                +PrimaryCapsules(32, 8, 9.p2, 2.p2)
                println("PC: ${image.shape}")
                +CapsuleLayer(10, 16, 3)
                println("Caps: ${image.shape}")
                +CapsuleStrength()

                +ActivationLayer(Activations.Softmax)

                +LossLayer(Losses.NegativeLogLikelihood)
            }

            outputs(image)

        }.build()
        model.init()


        val rngSeed = 12345
        val mnistTrain = MnistDataSetIterator(64, true, rngSeed)
        val mnistTest = MnistDataSetIterator(64, false, rngSeed)


        val nEpochs = 5

        (1..nEpochs).forEach { epoch ->
            val time = measureTimeMillis {
                try {
                    model.fit(mnistTrain)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            println("Epoch " + epoch + " complete, took ${time / 1000} seconds")
        }
        println(model.summary())

        val eval: Evaluation = model.evaluate(mnistTest)
        println(eval.stats())
    }

    @Test
    fun `test mnist`() {

        val model = graph {
            val image = input("image", 1, 28, 28)

            image.reshape(1, 28, 28)

            image {
                +Convolution2DLayer(20, 5.p2) { activation = ActivationLReLU() }
                +Subsampling2DLayer(kernelSize = 2.p2, stride = 2.p2)

                +Convolution2DLayer(20, 5.p2) { activation = ActivationLReLU() }
                +Subsampling2DLayer(kernelSize = 2.p2, stride = 2.p2)

                flatten()

                +DenseLayer(200) { activation = ActivationLReLU() }

                +OutputLayer(10, loss = LossNegativeLogLikelihood()) { activation = ActivationSoftmax() }
            }

            outputs(image)
        }.build()
        model.init()


        val rngSeed = 12345
        val mnistTrain = MnistDataSetIterator(64, true, rngSeed)
        val mnistTest = MnistDataSetIterator(64, false, rngSeed)


        val nEpochs = 5

        (1..nEpochs).forEach { epoch ->
            val time = measureTimeMillis {
                try {
                    model.fit(mnistTrain)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            println("Epoch " + epoch + " complete, took ${time / 1000} seconds")
        }
        println(model.summary())

        val eval: Evaluation = model.evaluate(mnistTest)
        println(eval.stats())

    }

}