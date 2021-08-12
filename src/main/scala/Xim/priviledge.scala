package Xim

import chisel3._

object priv_consts extends PriviledgeLevelConstants

class PriviledgeReg extends Module {
    val io = IO(new Bundle {
        val wen = Input(Bool())
        val wlevel = Input(UInt(2.W))
        val rlevel = Output(UInt(2.W))
    })
    
    val priviledge_level = RegInit(3.U(2.W)) //initialized as Machine level
    
    when(io.wen) {
        priviledge_level := io.wlevel
    }

    io.rlevel := priviledge_level
}

class PriviledgeSignal extends Module {
    val io = IO(new Bundle {
        val es_valid = Input(UInt(1.W))
        val es_ex = Input(UInt(1.W))
        val inst_mret = Input(UInt(1.W))
        val inst_sret = Input(UInt(1.W))

        val mstatus_mpp = Input(UInt(2.W))
        val sstatus_spp = Input(UInt(1.W))

        val priv_level = Output(UInt(2.W))
    })

    //priviledge level
    val priv_level = Module(new PriviledgeReg)
    val priv_wen = Wire(Bool())
    val priv_wlevel = Wire(UInt(2.W))
    val priv_rlevel = Wire(UInt(2.W))
    val next_priv_level = Wire(UInt(2.W))

    priv_level.io.wen := priv_wen
    priv_level.io.wlevel := priv_wlevel
    priv_rlevel := priv_level.io.rlevel
    priv_wlevel := next_priv_level

    io.priv_level := priv_rlevel

    when (io.es_valid === 1.U && io.inst_mret === 1.U && priv_rlevel === priv_consts.Machine) {
        next_priv_level := io.mstatus_mpp
    } .elsewhen (io.es_valid === 1.U && io.inst_sret === 1.U && priv_rlevel === priv_consts.Supervisor) {
        next_priv_level := io.sstatus_spp
    } .elsewhen (io.es_ex === 1.U) {
        next_priv_level := priv_consts.Machine
    } .otherwise {
        next_priv_level := priv_consts.Machine
    }

    when (io.es_valid === 1.U && (io.inst_mret === 1.U || io.inst_sret === 1.U)) {
        priv_wen := true.B
    } .elsewhen (io.es_ex === 1.U) {
        priv_wen := true.B
    } .otherwise {
        priv_wen := false.B
    }
}