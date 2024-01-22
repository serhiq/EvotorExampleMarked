package com.example.playgroundevotor

import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.playgroundevotor.databinding.ActivityMainBinding
import ru.evotor.framework.core.IntegrationManagerCallback
import ru.evotor.framework.core.IntegrationManagerFuture
import ru.evotor.framework.core.action.command.open_receipt_command.OpenSellReceiptCommand
import ru.evotor.framework.core.action.event.receipt.changes.position.PositionAdd
import ru.evotor.framework.inventory.ProductItem
import ru.evotor.framework.inventory.ProductQuery
import ru.evotor.framework.inventory.ProductTable
import ru.evotor.framework.inventory.ProductType
import ru.evotor.framework.receipt.Position
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val mark = "046062030995805;-Ub:'AB8AOue5"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        title = "Тестовое приложение для Evotor"
        binding.applyBtn.setOnClickListener { start() }
    }

    private fun start() {
        val haveApp = isPackageInstalled(this, "ru.evotor.evomark")
        if (!haveApp) {
            notifyUser("!!! Приложение продажа табака не установлено")
            return
        }

        val productMarked = searchMarkedTobacoo()
        if (productMarked == null) {
            notifyUser("!! Товар с типом Маркированый табак не найден. Пожалуйста создайте маркированный товар, и повторите.")
            return
        }

        val position =
            Position.Builder.newInstance(productMarked, BigDecimal.ONE).setMark(mark).build()

        val changes = listOf(PositionAdd(position))

        OpenSellReceiptCommand(changes, null, null).process(this,
            IntegrationManagerCallback { integrationManagerFuture ->
                try {
                    val result = integrationManagerFuture.result
                    if (IntegrationManagerFuture.Result.Type.ERROR == result.type) {
                        notifyUser("Ошибка формирования чека: " + result.error.message + ". Код: " + result.error.code)
                        return@IntegrationManagerCallback
                    }

                } catch (e: Exception) {
                    notifyUser(e.localizedMessage)
                }
            })
    }

    private fun notifyUser(msg: String) {
        binding.textView.text = binding.textView.text.toString() + "\n\n" + msg
    }

    private fun searchMarkedTobacoo(): ProductItem.Product? {
        return ProductQuery().addFieldFilter<Boolean>(ProductTable.ROW_IS_GROUP).equal(false).and(
                ProductQuery().type.equal(ProductType.TOBACCO_MARKED)
            ).execute(this).toList().filterIsInstance<ProductItem.Product>().firstOrNull()
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }
}